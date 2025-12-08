package com.milton.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable());

        return http.build();




//        http
//            .csrf(csrf -> csrf.disable())
//            .authorizeHttpRequests(auth -> auth
//                .requestMatchers("/", "/login", "/css/**", "/js/**", "/images/**").permitAll()
//                .anyRequest().authenticated()
//            )
//            .formLogin(login -> login
//                .loginPage("/login")        // show your custom login page
//                .defaultSuccessUrl("/", true) // redirect to / after successful login
//                .permitAll()
//            )
//            .logout(logout -> logout
//                .logoutUrl("/logout")
//                .logoutSuccessUrl("/")      // after logout → go back to index
//                .permitAll()
//            );
//
//        return http.build();
    }

//    @Bean
//    public org.springframework.security.core.userdetails.UserDetailsService users() {
//        var user = org.springframework.security.core.userdetails.User
//                .withUsername("test")
//                .password("{noop}test") // no password encoder for now
//                .roles("USER")
//                .build();
//
//        return new org.springframework.security.provisioning.InMemoryUserDetailsManager(user);
//    }

}
