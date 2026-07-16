package com.example.demo.security;

import com.example.demo.entity.Customer;
import com.example.demo.enums.Role;
import com.example.demo.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerUserDetailsServiceTest {

    @Mock private CustomerRepository customerRepository;
    @InjectMocks private CustomerUserDetailsService service;

    @Test
    void loadsExistingUserWithRole() {
        Customer c = new Customer();
        c.setEmail("a@b.com");
        c.setPassword("secret");
        c.setRole(Role.ADMIN);
        when(customerRepository.findByEmail("a@b.com")).thenReturn(Optional.of(c));

        UserDetails ud = service.loadUserByUsername("a@b.com");
        assertEquals("a@b.com", ud.getUsername());
        assertTrue(ud.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void nullRoleDefaultsToUser() {
        Customer c = new Customer();
        c.setEmail("a@b.com");
        c.setPassword("secret");
        c.setRole(null);
        when(customerRepository.findByEmail("a@b.com")).thenReturn(Optional.of(c));

        UserDetails ud = service.loadUserByUsername("a@b.com");
        assertTrue(ud.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void missingUser_throws() {
        when(customerRepository.findByEmail("none@x.com")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("none@x.com"));
    }
}

