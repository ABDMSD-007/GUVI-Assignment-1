package com.example.demo.security;

import com.example.demo.entity.Customer;
import com.example.demo.enums.Role;
import com.example.demo.repository.CustomerRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Task 8 - UserDetailsService backed by Customer table (email = username).
 */
@Service
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    public CustomerUserDetailsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        Role role = customer.getRole() == null ? Role.USER : customer.getRole();
        return User.withUsername(customer.getEmail())
                .password(customer.getPassword())
                .roles(role.name())
                .build();
    }
}

