package org.northernarc.assessment4;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.northernarc.assessment4.controller.BankController;
import org.northernarc.assessment4.controller.AuthController;
import org.northernarc.assessment4.dto.BranchBalance;
import org.northernarc.assessment4.dto.CustomerSummaryDTO;
import org.northernarc.assessment4.dto.DashboardResponse;
import org.northernarc.assessment4.exception.AccountNotFoundException;
import org.northernarc.assessment4.exception.CustomerNotFoundException;
import org.northernarc.assessment4.exception.DuplicateResourceException;
import org.northernarc.assessment4.exception.GlobalExceptionHandler;
import org.northernarc.assessment4.model.Account;
import org.northernarc.assessment4.model.Customer;
import org.northernarc.assessment4.model.Transaction;
import org.northernarc.assessment4.repository.AccountRepository;
import org.northernarc.assessment4.repository.CustomerRepository;
import org.northernarc.assessment4.repository.TransactionRepository;
import org.northernarc.assessment4.security.JwtUtil;
import org.northernarc.assessment4.service.BankService;
import org.northernarc.assessment4.serviceimpl.BankServiceImpl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Exhaustive "hidden-style" suite: JUnit 5 + Mockito + MockMvc, no database.
 *  - Service    -> pure Mockito (@Mock repositories, @InjectMocks service)
 *  - Controller -> standalone MockMvc with a mocked BankService
 *  - JwtUtil / Bean Validation / DTOs -> plain unit tests
 *
 * @PreAuthorize role rules and the 401 entry point are covered by the
 * @SpringBootTest integration tests (standalone MockMvc has no security filter chain).
 */
class BankApplicationHiddenTests {

    static Customer newCustomer(Long id, String name, String email, String branch) {
        Customer c = new Customer();
        c.setCustomerId(id);
        c.setCustomerName(name);
        c.setEmail(email);
        c.setPassword("password123");
        c.setBranch(branch);
        c.setRole("USER");
        return c;
    }

    static Account newAccount(String number, String type, double balance) {
        Account a = new Account();
        a.setAccountNumber(number);
        a.setAccountType(type);
        a.setBalance(balance);
        return a;
    }

    static Transaction newTransaction(Long id, double amount, String type, LocalDate date) {
        Transaction t = new Transaction();
        t.setTransactionId(id);
        t.setAmount(amount);
        t.setTransactionType(type);
        t.setTransactionDate(date);
        return t;
    }

    // ============ 1) SERVICE LAYER — pure Mockito ============
    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("Service Layer :: BankServiceImpl (Mockito)")
    class ServiceLayerTests {

        @Mock private CustomerRepository customerRepository;
        @Mock private AccountRepository accountRepository;
        @Mock private TransactionRepository transactionRepository;
        @InjectMocks private BankServiceImpl service;

        /** Builds a lightweight BranchBalance projection stub without touching Object[]. */
        private BranchBalance branchBalance(String branch, Double total) {
            return new BranchBalance() {
                @Override public String getBranch() { return branch; }
                @Override public Double getTotalBalance() { return total; }
            };
        }

        @Test
        @DisplayName("saveCustomer delegates and returns saved entity")
        void saveCustomer_happyPath() {
            Customer input = newCustomer(null, "Rahul", "rahul@bank.com", "Chennai");
            Customer saved = newCustomer(1L, "Rahul", "rahul@bank.com", "Chennai");
            // New customer (null id) => service first checks the email is not already taken.
            when(customerRepository.findByEmail("rahul@bank.com")).thenReturn(Optional.empty());
            when(customerRepository.save(input)).thenReturn(saved);

            Customer result = service.saveCustomer(input);

            assertThat(result).isSameAs(saved);
            assertThat(result.getCustomerId()).isEqualTo(1L);
            verify(customerRepository, times(1)).findByEmail("rahul@bank.com");
            verify(customerRepository, times(1)).save(input);
            verifyNoMoreInteractions(customerRepository);
            verifyNoInteractions(accountRepository, transactionRepository);
        }

        @Test
        @DisplayName("saveCustomer captures the entity passed in")
        void saveCustomer_argumentCaptor() {
            Customer input = newCustomer(null, "Asha", "asha@bank.com", "Delhi");
            when(customerRepository.save(any(Customer.class))).thenReturn(input);

            service.saveCustomer(input);

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo("asha@bank.com");
        }

        @Test
        @DisplayName("saveCustomer propagates repository exceptions")
        void saveCustomer_repositoryThrows() {
            Customer input = newCustomer(null, "X", "x@bank.com", "Pune");
            when(customerRepository.save(any(Customer.class))).thenThrow(new RuntimeException("DB down"));

            assertThatThrownBy(() -> service.saveCustomer(input))
                    .isInstanceOf(RuntimeException.class).hasMessage("DB down");
        }

        @Test
        @DisplayName("saveCustomer rejects a duplicate email with DuplicateResourceException and never saves")
        void saveCustomer_duplicateEmail() {
            Customer input = newCustomer(null, "Rahul", "rahul@bank.com", "Chennai");
            Customer existing = newCustomer(99L, "Existing", "rahul@bank.com", "Chennai");
            when(customerRepository.findByEmail("rahul@bank.com")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.saveCustomer(input))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("rahul@bank.com");

            verify(customerRepository, times(1)).findByEmail("rahul@bank.com");
            verify(customerRepository, never()).save(any(Customer.class));
        }

        @Test
        @DisplayName("saveCustomer with an existing id skips the duplicate-email check")
        void saveCustomer_existingIdSkipsDuplicateCheck() {
            Customer input = newCustomer(5L, "Rahul", "rahul@bank.com", "Chennai");
            when(customerRepository.save(input)).thenReturn(input);

            service.saveCustomer(input);

            verify(customerRepository, never()).findByEmail(any());
            verify(customerRepository, times(1)).save(input);
        }

        @Test
        @DisplayName("saveAccount delegates to repository")
        void saveAccount_happyPath() {
            Account input = newAccount("ACC1", "SAVINGS", 100.0);
            when(accountRepository.save(input)).thenReturn(input);

            assertThat(service.saveAccount(input)).isSameAs(input);
            verify(accountRepository).save(input);
            verifyNoInteractions(customerRepository, transactionRepository);
        }

        @Test
        @DisplayName("deleteAccount deletes when account exists")
        void deleteAccount_exists() {
            when(accountRepository.existsById("ACC1")).thenReturn(true);

            service.deleteAccount("ACC1");

            InOrder inOrder = Mockito.inOrder(accountRepository);
            inOrder.verify(accountRepository).existsById("ACC1");
            inOrder.verify(accountRepository).deleteById("ACC1");
            verifyNoMoreInteractions(accountRepository);
        }

        @Test
        @DisplayName("deleteAccount throws when missing and never deletes")
        void deleteAccount_missing() {
            when(accountRepository.existsById("NOPE")).thenReturn(false);

            assertThatThrownBy(() -> service.deleteAccount("NOPE"))
                    .isInstanceOf(AccountNotFoundException.class).hasMessageContaining("NOPE");

            verify(accountRepository).existsById("NOPE");
            verify(accountRepository, never()).deleteById(anyString());
        }

        @Test
        @DisplayName("getAccountsByType returns list")
        void getAccountsByType_list() {
            when(accountRepository.findByAccountType("SAVINGS"))
                    .thenReturn(List.of(newAccount("A1", "SAVINGS", 10.0)));
            assertThat(service.getAccountsByType("SAVINGS")).hasSize(1);
            verify(accountRepository).findByAccountType("SAVINGS");
        }

        @Test
        @DisplayName("getAccountsByType returns empty (size == 0)")
        void getAccountsByType_empty() {
            when(accountRepository.findByAccountType("FD")).thenReturn(Collections.emptyList());
            List<Account> result = service.getAccountsByType("FD");
            assertThat(result).isEmpty();
            assertThat(result.size()).isEqualTo(0);
        }

        @Test
        @DisplayName("getCustomersByBranch delegates")
        void getCustomersByBranch() {
            when(customerRepository.findByBranch("Chennai"))
                    .thenReturn(List.of(newCustomer(1L, "R", "r@b.com", "Chennai")));
            assertThat(service.getCustomersByBranch("Chennai")).hasSize(1);
        }

        @Test
        @DisplayName("getTransactionsByType delegates")
        void getTransactionsByType() {
            when(transactionRepository.findByTransactionType("CREDIT"))
                    .thenReturn(List.of(newTransaction(1L, 5.0, "CREDIT", LocalDate.now())));
            assertThat(service.getTransactionsByType("CREDIT")).hasSize(1);
        }

        @Test
        @DisplayName("getAccountsWithBalanceGreaterThan delegates with threshold")
        void getAccountsWithBalanceGreaterThan() {
            when(accountRepository.findByBalanceGreaterThan(100000.0))
                    .thenReturn(List.of(newAccount("A2", "CURRENT", 150000.0)));
            assertThat(service.getAccountsWithBalanceGreaterThan(100000.0)).hasSize(1);
        }

        @Test
        @DisplayName("getRichCustomers delegates")
        void getRichCustomers() {
            when(customerRepository.findRichCustomers(100000.0))
                    .thenReturn(List.of(newCustomer(1L, "Rahul", "r@b.com", "Chennai")));
            assertThat(service.getRichCustomers(100000.0))
                    .extracting(Customer::getCustomerName).containsExactly("Rahul");
        }

        @Test
        @DisplayName("getTotalBalancePerBranch maps typed projection rows")
        void getTotalBalancePerBranch_maps() {
            when(customerRepository.findBranchBalances())
                    .thenReturn(List.of(branchBalance("Chennai", 200000.0), branchBalance("Delhi", 50000.0)));
            Map<String, Double> result = service.getTotalBalancePerBranch();
            assertThat(result).containsEntry("Chennai", 200000.0).containsEntry("Delhi", 50000.0).hasSize(2);
        }

        @Test
        @DisplayName("getTotalBalancePerBranch treats null SUM as 0.0")
        void getTotalBalancePerBranch_nullSum() {
            when(customerRepository.findBranchBalances())
                    .thenReturn(Collections.singletonList(branchBalance("Empty", null)));
            assertThat(service.getTotalBalancePerBranch().get("Empty")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("getTotalBalancePerBranch empty map when no rows")
        void getTotalBalancePerBranch_empty() {
            when(customerRepository.findBranchBalances()).thenReturn(Collections.emptyList());
            assertThat(service.getTotalBalancePerBranch()).isEmpty();
        }

        @Test
        @DisplayName("getCustomersWithMultipleAccounts delegates")
        void getCustomersWithMultipleAccounts() {
            when(customerRepository.findCustomersWithMultipleAccounts())
                    .thenReturn(List.of(newCustomer(1L, "R", "r@b.com", "Chennai")));
            assertThat(service.getCustomersWithMultipleAccounts()).hasSize(1);
        }

        @Test
        @DisplayName("getLatestTransaction returns first element, requests page(0,1)")
        void getLatestTransaction_found() {
            Transaction latest = newTransaction(9L, 1200.0, "CREDIT", LocalDate.now());
            when(transactionRepository.findLatestTransaction(any(Pageable.class))).thenReturn(List.of(latest));

            assertThat(service.getLatestTransaction()).isSameAs(latest);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(transactionRepository).findLatestTransaction(captor.capture());
            assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
            assertThat(captor.getValue().getPageSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("getLatestTransaction returns null on empty list")
        void getLatestTransaction_none() {
            when(transactionRepository.findLatestTransaction(any(Pageable.class))).thenReturn(Collections.emptyList());
            assertThat(service.getLatestTransaction()).isNull();
        }

        @Test
        @DisplayName("getAccountsWithNoTransactions delegates")
        void getAccountsWithNoTransactions() {
            when(accountRepository.findAccountsWithNoTransactions())
                    .thenReturn(List.of(newAccount("A1", "SAVINGS", 10.0), newAccount("A2", "CURRENT", 20.0)));
            assertThat(service.getAccountsWithNoTransactions()).hasSize(2);
        }

        @Test
        @DisplayName("getAccountsWithNoTransactions empty (size == 0)")
        void getAccountsWithNoTransactions_empty() {
            when(accountRepository.findAccountsWithNoTransactions()).thenReturn(Collections.emptyList());
            assertThat(service.getAccountsWithNoTransactions().size()).isEqualTo(0);
        }

        @Test
        @DisplayName("increaseAccountBalance succeeds when one row updated")
        void increaseBalance_updated() {
            when(accountRepository.increaseBalance("ACC1", 5000.0)).thenReturn(1);
            service.increaseAccountBalance("ACC1", 5000.0);
            verify(accountRepository).increaseBalance("ACC1", 5000.0);
        }

        @Test
        @DisplayName("increaseAccountBalance throws when zero rows updated (== 0)")
        void increaseBalance_notFound() {
            when(accountRepository.increaseBalance("NOPE", 5000.0)).thenReturn(0);
            assertThatThrownBy(() -> service.increaseAccountBalance("NOPE", 5000.0))
                    .isInstanceOf(AccountNotFoundException.class).hasMessageContaining("NOPE");
        }

        @Test
        @DisplayName("increaseAccountBalance works with zero amount if account exists")
        void increaseBalance_zeroAmount() {
            when(accountRepository.increaseBalance("ACC1", 0.0)).thenReturn(1);
            service.increaseAccountBalance("ACC1", 0.0);
            verify(accountRepository).increaseBalance("ACC1", 0.0);
        }

        @Test
        @DisplayName("getAllAccountsPaginated delegates the Pageable")
        void getAllAccountsPaginated() {
            Pageable pageable = PageRequest.of(0, 10);
            when(accountRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(newAccount("A1", "SAVINGS", 10.0))));
            assertThat(service.getAllAccountsPaginated(pageable).getTotalElements()).isEqualTo(1);
            verify(accountRepository).findAll(pageable);
        }

        @Test
        @DisplayName("getCustomerSummary returns DTO when present")
        void getCustomerSummary_found() {
            when(customerRepository.findCustomerSummary(1L))
                    .thenReturn(Optional.of(new CustomerSummaryDTO("Rahul", "Chennai", 2L, 200000.0)));
            CustomerSummaryDTO result = service.getCustomerSummary(1L);
            assertThat(result.customerName()).isEqualTo("Rahul");
            assertThat(result.numberOfAccounts()).isEqualTo(2L);
            assertThat(result.totalBalance()).isEqualTo(200000.0);
        }

        @Test
        @DisplayName("getCustomerSummary throws when Optional empty")
        void getCustomerSummary_missing() {
            when(customerRepository.findCustomerSummary(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getCustomerSummary(999L))
                    .isInstanceOf(CustomerNotFoundException.class).hasMessageContaining("999");
        }

        @Test
        @DisplayName("getDashboardMetrics assembles metrics")
        void dashboard_happyPath() {
            when(customerRepository.count()).thenReturn(120L);
            when(accountRepository.count()).thenReturn(245L);
            when(accountRepository.findTotalBalance()).thenReturn(45000000.0);
            when(customerRepository.findBranchesRankedByBalance(any(Pageable.class)))
                    .thenReturn(List.of("Chennai", "Delhi"));
            when(customerRepository.findCustomersRankedByBalance(any(Pageable.class)))
                    .thenReturn(List.of("Rahul Sharma", "Asha"));

            DashboardResponse r = service.getDashboardMetrics();
            assertThat(r.totalCustomers()).isEqualTo(120L);
            assertThat(r.totalAccounts()).isEqualTo(245L);
            assertThat(r.totalBalance()).isEqualTo(45000000.0);
            assertThat(r.topBranch()).isEqualTo("Chennai");
            assertThat(r.highestBalanceCustomer()).isEqualTo("Rahul Sharma");
        }

        @Test
        @DisplayName("getDashboardMetrics: empty DB -> zeros, null SUM -> 0.0, null tops")
        void dashboard_emptyDb() {
            when(customerRepository.count()).thenReturn(0L);
            when(accountRepository.count()).thenReturn(0L);
            when(accountRepository.findTotalBalance()).thenReturn(null);
            when(customerRepository.findBranchesRankedByBalance(any(Pageable.class))).thenReturn(Collections.emptyList());
            when(customerRepository.findCustomersRankedByBalance(any(Pageable.class))).thenReturn(Collections.emptyList());

            DashboardResponse r = service.getDashboardMetrics();
            assertThat(r.totalCustomers()).isEqualTo(0L);
            assertThat(r.totalBalance()).isEqualTo(0.0);
            assertThat(r.topBranch()).isNull();
            assertThat(r.highestBalanceCustomer()).isNull();
        }

        @Test
        @DisplayName("getDashboardMetrics avoids N+1 (no per-entity calls)")
        void dashboard_noNPlusOne() {
            when(customerRepository.count()).thenReturn(2L);
            when(accountRepository.count()).thenReturn(3L);
            when(accountRepository.findTotalBalance()).thenReturn(200000.0);
            when(customerRepository.findBranchesRankedByBalance(any(Pageable.class))).thenReturn(List.of("Chennai"));
            when(customerRepository.findCustomersRankedByBalance(any(Pageable.class))).thenReturn(List.of("Rahul Sharma"));

            service.getDashboardMetrics();

            verify(customerRepository).count();
            verify(accountRepository).count();
            verify(accountRepository).findTotalBalance();
            verify(customerRepository).findBranchesRankedByBalance(any(Pageable.class));
            verify(customerRepository).findCustomersRankedByBalance(any(Pageable.class));
            verifyNoMoreInteractions(customerRepository, accountRepository);
            verifyNoInteractions(transactionRepository);
        }
    }

    // ============ 2) CONTROLLER — standalone MockMvc ============
    @Nested
    @DisplayName("Controller Layer :: BankController (MockMvc standalone)")
    class ControllerLayerTests {

        private BankService bankService;
        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
            bankService = Mockito.mock(BankService.class);
            LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
            validator.afterPropertiesSet();
            mockMvc = MockMvcBuilders.standaloneSetup(new BankController(bankService))
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .setValidator(validator)
                    .build();
        }

        @Test
        @DisplayName("POST /api/customers -> 201, hides password")
        void createCustomer_created() throws Exception {
            when(bankService.saveCustomer(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
            String body = "{\"customerName\":\"Rahul Sharma\",\"email\":\"rahul@bank.com\",\"password\":\"password123\",\"branch\":\"Chennai\"}";
            mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.customerName").value("Rahul Sharma"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @DisplayName("POST /api/customers invalid -> 400 with errors")
        void createCustomer_invalid() throws Exception {
            String body = "{\"customerName\":\"\",\"email\":\"not-an-email\",\"password\":\"123\",\"branch\":\"\"}";
            mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(1))));
            verify(bankService, never()).saveCustomer(any());
        }

        @Test
        @DisplayName("POST /api/accounts -> 201, hides customer")
        void createAccount_created() throws Exception {
            when(bankService.saveAccount(any(Account.class))).thenAnswer(i -> i.getArgument(0));
            String body = "{\"accountNumber\":\"ACC1001\",\"accountType\":\"SAVINGS\",\"balance\":50000.0}";
            mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accountNumber").value("ACC1001"))
                    .andExpect(jsonPath("$.customer").doesNotExist());
        }

        @Test
        @DisplayName("POST /api/accounts negative balance -> 400")
        void createAccount_negativeBalance() throws Exception {
            String body = "{\"accountNumber\":\"ACC9999\",\"accountType\":\"SAVINGS\",\"balance\":-100.0}";
            mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
            verify(bankService, never()).saveAccount(any());
        }

        @Test
        @DisplayName("GET /api/accounts -> 200 page content")
        void getAllAccounts_paged() throws Exception {
            List<Account> pageContent = List.of(
                    newAccount("ACC1002", "CURRENT", 150000.0), newAccount("ACC1001", "SAVINGS", 50000.0));
            when(bankService.getAllAccountsPaginated(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(pageContent, PageRequest.of(0, 10), pageContent.size()));
            mockMvc.perform(get("/api/accounts").param("page", "0").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].accountNumber").value("ACC1002"));
        }

        // ---- Additional HTTP status-code edge cases ----
        @Test
        @DisplayName("POST /api/customers malformed JSON -> 400")
        void createCustomer_malformedJson() throws Exception {
            mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                    .andExpect(status().isBadRequest());
            verify(bankService, never()).saveCustomer(any());
        }

        @Test
        @DisplayName("POST /api/customers missing body -> 400")
        void createCustomer_missingBody() throws Exception {
            mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/customers wrong content type -> 415")
        void createCustomer_unsupportedMediaType() throws Exception {
            mockMvc.perform(post("/api/customers").contentType(MediaType.TEXT_PLAIN).content("hello"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("Unmapped method on /api/customers -> 405")
        void customers_methodNotAllowed() throws Exception {
            mockMvc.perform(delete("/api/customers"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("PUT /api/accounts/{id}/balance with non-numeric amount -> 400")
        void updateBalance_typeMismatch() throws Exception {
            mockMvc.perform(put("/api/accounts/ACC1/balance").param("amount", "abc"))
                    .andExpect(status().isBadRequest());
            verify(bankService, never()).increaseAccountBalance(anyString(), org.mockito.ArgumentMatchers.anyDouble());
        }

        @Test
        @DisplayName("PUT /api/accounts/{id}/balance -> 200")
        void updateBalance_ok() throws Exception {
            mockMvc.perform(put("/api/accounts/ACC1002/balance").param("amount", "1000.0")).andExpect(status().isOk());
            verify(bankService).increaseAccountBalance("ACC1002", 1000.0);
        }

        @Test
        @DisplayName("PUT /api/accounts/{id}/balance missing amount -> 400")
        void updateBalance_missingParam() throws Exception {
            mockMvc.perform(put("/api/accounts/ACC1002/balance")).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/accounts/view/{id} -> 200 list")
        void viewAccountsByType_ok() throws Exception {
            when(bankService.getAccountsByType("SAVINGS")).thenReturn(List.of(newAccount("ACC1001", "SAVINGS", 50000.0)));
            mockMvc.perform(get("/api/accounts/view/ACC1001").param("type", "SAVINGS"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].accountNumber").value("ACC1001"));
        }

        @Test
        @DisplayName("GET /api/customers/{id}/summary -> 200")
        void summary_ok() throws Exception {
            when(bankService.getCustomerSummary(1L)).thenReturn(new CustomerSummaryDTO("Rahul Sharma", "Chennai", 2L, 200000.0));
            mockMvc.perform(get("/api/customers/1/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.customerName").value("Rahul Sharma"))
                    .andExpect(jsonPath("$.numberOfAccounts").value(2))
                    .andExpect(jsonPath("$.totalBalance").value(200000.0));
        }

        @Test
        @DisplayName("GET /api/customers/{id}/summary -> 404 on CustomerNotFoundException")
        void summary_notFound() throws Exception {
            when(bankService.getCustomerSummary(999L)).thenThrow(new CustomerNotFoundException("Customer not found with id: 999"));
            mockMvc.perform(get("/api/customers/999/summary"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("GET /api/customers/rich -> 200")
        void rich_ok() throws Exception {
            when(bankService.getRichCustomers(100000.0)).thenReturn(List.of(newCustomer(1L, "Rahul Sharma", "r@b.com", "Chennai")));
            mockMvc.perform(get("/api/customers/rich").param("threshold", "100000.0"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].customerName").value("Rahul Sharma"));
        }

        @Test
        @DisplayName("GET /api/customers/rich missing threshold -> 400")
        void rich_missingParam() throws Exception {
            mockMvc.perform(get("/api/customers/rich")).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/customers/rich non-numeric threshold -> 400")
        void rich_typeMismatch() throws Exception {
            mockMvc.perform(get("/api/customers/rich").param("threshold", "abc")).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/branches/balances -> 200 map")
        void branchBalances_ok() throws Exception {
            when(bankService.getTotalBalancePerBranch()).thenReturn(Map.of("Chennai", 200000.0));
            mockMvc.perform(get("/api/branches/balances"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.Chennai").value(200000.0));
        }

        @Test
        @DisplayName("GET /api/customers/multiple-accounts -> 200")
        void multipleAccounts_ok() throws Exception {
            when(bankService.getCustomersWithMultipleAccounts()).thenReturn(List.of(newCustomer(1L, "Rahul", "r@b.com", "Chennai")));
            mockMvc.perform(get("/api/customers/multiple-accounts")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("GET /api/transactions/latest -> 200")
        void latest_ok() throws Exception {
            when(bankService.getLatestTransaction()).thenReturn(newTransaction(9L, 1200.0, "CREDIT", LocalDate.now()));
            mockMvc.perform(get("/api/transactions/latest"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(1200.0));
        }

        @Test
        @DisplayName("GET /api/accounts/idle -> 200")
        void idle_ok() throws Exception {
            when(bankService.getAccountsWithNoTransactions())
                    .thenReturn(List.of(newAccount("A1", "SAVINGS", 10.0), newAccount("A2", "CURRENT", 20.0)));
            mockMvc.perform(get("/api/accounts/idle")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("GET /api/dashboard -> 200 all fields")
        void dashboard_ok() throws Exception {
            when(bankService.getDashboardMetrics()).thenReturn(new DashboardResponse(120L, 245L, 45000000.0, "Chennai", "Rahul Sharma"));
            mockMvc.perform(get("/api/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCustomers").value(120))
                    .andExpect(jsonPath("$.topBranch").value("Chennai"))
                    .andExpect(jsonPath("$.highestBalanceCustomer").value("Rahul Sharma"));
        }
    }

    // ============ 3) JWT ============
    @Nested
    @DisplayName("Security :: JwtUtil")
    class JwtUtilTests {
        private final JwtUtil jwtUtil = new JwtUtil();
        private UserDetails user(String u) { return new User(u, "pwd", List.of(new SimpleGrantedAuthority("ROLE_USER"))); }

        @Test void generate() { assertThat(jwtUtil.generateToken(user("rahul@bank.com"))).startsWith("eyJ"); }

        @Test void extractUsername() {
            String t = jwtUtil.generateToken(user("rahul@bank.com"));
            assertThat(jwtUtil.extractUsername(t)).isEqualTo("rahul@bank.com");
        }

        @Test void validate_true() {
            UserDetails ud = user("rahul@bank.com");
            assertThat(jwtUtil.validateToken(jwtUtil.generateToken(ud), ud)).isTrue();
        }

        @Test void validate_wrongUser() {
            String t = jwtUtil.generateToken(user("rahul@bank.com"));
            assertThat(jwtUtil.validateToken(t, user("other@bank.com"))).isFalse();
        }

        @Test void malformed() {
            assertThatThrownBy(() -> jwtUtil.extractUsername("this.is.not.a.jwt")).isInstanceOf(Exception.class);
        }

        @Test void expirationFuture() {
            String t = jwtUtil.generateToken(user("rahul@bank.com"));
            assertThat(jwtUtil.extractExpiration(t)).isAfter(new java.util.Date());
        }
    }

    // ============ 4) BEAN VALIDATION ============
    @Nested
    @DisplayName("Task 2 :: Bean Validation")
    class BeanValidationTests {
        private Validator validator;
        @BeforeEach void setUp() { validator = Validation.buildDefaultValidatorFactory().getValidator(); }

        @Test void customer_valid() {
            Set<ConstraintViolation<Customer>> v = validator.validate(newCustomer(null, "Rahul", "rahul@bank.com", "Chennai"));
            assertThat(v).isEmpty();
            assertThat(v.size()).isEqualTo(0);
        }
        @Test void customer_blankName() { assertThat(validator.validate(newCustomer(null, "", "rahul@bank.com", "Chennai"))).isNotEmpty(); }
        @Test void customer_nullName() { assertThat(validator.validate(newCustomer(null, null, "rahul@bank.com", "Chennai"))).isNotEmpty(); }
        @Test void customer_badEmail() {
            assertThat(validator.validate(newCustomer(null, "Rahul", "bad", "Chennai")))
                    .anyMatch(x -> x.getPropertyPath().toString().equals("email"));
        }
        @Test void customer_shortPassword() {
            Customer c = newCustomer(null, "Rahul", "rahul@bank.com", "Chennai"); c.setPassword("123");
            assertThat(validator.validate(c)).anyMatch(x -> x.getPropertyPath().toString().equals("password"));
        }
        @Test void customer_passwordBoundary() {
            Customer c = newCustomer(null, "Rahul", "rahul@bank.com", "Chennai"); c.setPassword("123456");
            assertThat(validator.validate(c)).isEmpty();
        }
        @Test void customer_blankBranch() {
            assertThat(validator.validate(newCustomer(null, "Rahul", "rahul@bank.com", "")))
                    .anyMatch(x -> x.getPropertyPath().toString().equals("branch"));
        }

        @Test void account_valid() { assertThat(validator.validate(newAccount("ACC1", "SAVINGS", 100.0))).isEmpty(); }
        @Test void account_negativeBalance() {
            assertThat(validator.validate(newAccount("ACC1", "SAVINGS", -100.0)))
                    .anyMatch(x -> x.getPropertyPath().toString().equals("balance"));
        }
        @Test void account_zeroBalance() { assertThat(validator.validate(newAccount("ACC1", "SAVINGS", 0.0))).isEmpty(); }
        @Test void account_nullBalance() {
            Account a = new Account(); a.setAccountNumber("ACC1"); a.setAccountType("SAVINGS"); a.setBalance(null);
            assertThat(validator.validate(a)).anyMatch(x -> x.getPropertyPath().toString().equals("balance"));
        }
        @Test void account_blankFields() {
            Set<ConstraintViolation<Account>> v = validator.validate(newAccount("", "", 10.0));
            assertThat(v).anyMatch(x -> x.getPropertyPath().toString().equals("accountNumber"));
            assertThat(v).anyMatch(x -> x.getPropertyPath().toString().equals("accountType"));
        }

        @Test void transaction_valid() { assertThat(validator.validate(newTransaction(null, 100.0, "CREDIT", LocalDate.now()))).isEmpty(); }
        @Test void transaction_nonPositiveAmount() {
            assertThat(validator.validate(newTransaction(null, 0.0, "CREDIT", LocalDate.now())))
                    .anyMatch(x -> x.getPropertyPath().toString().equals("amount"));
        }
        @Test void transaction_nullDate() {
            assertThat(validator.validate(newTransaction(null, 100.0, "CREDIT", null)))
                    .anyMatch(x -> x.getPropertyPath().toString().equals("transactionDate"));
        }
        @Test void transaction_blankType() {
            assertThat(validator.validate(newTransaction(null, 100.0, "", LocalDate.now())))
                    .anyMatch(x -> x.getPropertyPath().toString().equals("transactionType"));
        }
    }

    // ============ 5) DTO ============
    @Nested
    @DisplayName("DTO :: records")
    class DtoTests {
        @Test void summary_nullCoalescing() {
            CustomerSummaryDTO dto = new CustomerSummaryDTO("Rahul", "Chennai", null, null);
            assertThat(dto.numberOfAccounts()).isEqualTo(0L);
            assertThat(dto.totalBalance()).isEqualTo(0.0);
        }
        @Test void summary_values() {
            CustomerSummaryDTO dto = new CustomerSummaryDTO("Rahul", "Chennai", 2L, 200000.0);
            assertThat(dto.customerName()).isEqualTo("Rahul");
            assertThat(dto.numberOfAccounts()).isEqualTo(2L);
        }
        @Test void dashboard_fields() {
            DashboardResponse full = new DashboardResponse(120L, 245L, 45000000.0, "Chennai", "Rahul Sharma");
            assertThat(full.topBranch()).isEqualTo("Chennai");
            DashboardResponse empty = new DashboardResponse(0L, 0L, 0.0, null, null);
            assertThat(empty.topBranch()).isNull();
        }
    }

    // ============ 6) AUTH CONTROLLER — login status codes ============
    @Nested
    @DisplayName("Task 8 :: AuthController /api/auth/login (MockMvc standalone)")
    class AuthControllerTests {

        private AuthenticationManager authenticationManager;
        private UserDetailsService userDetailsService;
        private JwtUtil jwtUtil;
        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
            authenticationManager = Mockito.mock(AuthenticationManager.class);
            userDetailsService = Mockito.mock(UserDetailsService.class);
            jwtUtil = Mockito.mock(JwtUtil.class);
            mockMvc = MockMvcBuilders
                    .standaloneSetup(new AuthController(authenticationManager, userDetailsService, jwtUtil))
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();
        }

        @Test
        @DisplayName("POST /api/auth/login valid credentials -> 200 with token")
        void login_success() throws Exception {
            UserDetails ud = new User("rahul@bank.com", "hashed", List.of(new SimpleGrantedAuthority("ROLE_USER")));
            Authentication auth = Mockito.mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(ud);
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtUtil.generateToken(ud)).thenReturn("eyJhbGciOiJIUzI1NiJ9.payload.sig");

            String body = "{\"email\":\"rahul@bank.com\",\"password\":\"password123\"}";
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("eyJhbGciOiJIUzI1NiJ9.payload.sig"));
        }

        @Test
        @DisplayName("POST /api/auth/login bad credentials -> 401")
        void login_badCredentials() throws Exception {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            String body = "{\"email\":\"rahul@bank.com\",\"password\":\"wrong\"}";
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));
            verify(jwtUtil, never()).generateToken(any());
        }

        @Test
        @DisplayName("POST /api/auth/login malformed JSON -> 400")
        void login_malformedJson() throws Exception {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{ bad"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(authenticationManager);
        }

        @Test
        @DisplayName("POST /api/auth/login wrong content type -> 415")
        void login_unsupportedMediaType() throws Exception {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.TEXT_PLAIN).content("hello"))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }
}
