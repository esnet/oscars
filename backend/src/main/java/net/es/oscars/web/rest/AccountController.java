package net.es.oscars.web.rest;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.exc.StartupException;
import net.es.oscars.security.db.UserRepository;
import net.es.oscars.security.ent.User;
import net.es.oscars.security.jwt.JwtAuthenticationRequest;
import net.es.oscars.security.jwt.JwtAuthenticationResponse;
import net.es.oscars.security.jwt.JwtTokenUtil;
import net.es.oscars.web.beans.PasswordChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;


@Slf4j
@Controller
public class AccountController {

    private AuthenticationManager authenticationManager;

    private JwtTokenUtil jwtTokenUtil;

    private UserDetailsService userDetailsService;

    private UserRepository userRepo;
    @ExceptionHandler(StartupException.class)
    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    public void handleStartup(StartupException ex) {
        log.warn("Still in startup");
    }

    @Autowired
    public AccountController(UserRepository userRepo, AuthenticationManager authenticationManager,
                             JwtTokenUtil jwtTokenUtil, UserDetailsService userDetailsService) {
        this.userRepo = userRepo;
        this.jwtTokenUtil = jwtTokenUtil;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }


    @RequestMapping(value = "/api/account/login", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtAuthenticationRequest authenticationRequest)
            throws AuthenticationException {

        log.info("logging in " + authenticationRequest.getUsername());
        // Perform the security
        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authenticationRequest.getUsername(),
                        authenticationRequest.getPassword()
                )
        );
        log.info("authenticated " + authenticationRequest.getUsername());
        boolean isAdmin = false;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("ADMIN")) {
                isAdmin = true;
            }
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("set authentication for " + authenticationRequest.getUsername());

        // Get user details post-security so we can generate token
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
        log.info("loaded details for " + authenticationRequest.getUsername());
        final String token = jwtTokenUtil.generateToken(userDetails);

        // Return the token
        log.info("returning token " + authenticationRequest.getUsername()+" : "+token);
        return ResponseEntity.ok(JwtAuthenticationResponse.builder().admin(isAdmin).token(token).build());
    }


    @RequestMapping(value = "/protected/account", method = RequestMethod.GET)
    @ResponseBody
    public User protected_account(Authentication authentication) {
        String username = authentication.getName();
        return userRepo.findByUsername(username).orElseThrow(NoSuchElementException::new);
    }

    @RequestMapping(value = "/protected/account_password", method = RequestMethod.POST)
    @ResponseBody
    public void protected_account_password(Authentication authentication,
                                           @RequestBody PasswordChange passwordChange) throws AuthenticationException{
        String username = authentication.getName();

        String newPassword = passwordChange.getNewPassword();
        String oldPassword = passwordChange.getOldPassword();

        // Perform the security
        final Authentication checkOldPassword = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,
                        oldPassword
                )
        );
        if (checkOldPassword.isAuthenticated()) {
            User dbUser = userRepo.findByUsername(username).orElseThrow(NoSuchElementException::new);
            String encodedPassword = new BCryptPasswordEncoder().encode(newPassword);
            dbUser.setPassword(encodedPassword);
            userRepo.save(dbUser);
        } else {
            throw new BadCredentialsException("invalid old password");

        }

    }

    @RequestMapping(value = "/protected/account", method = RequestMethod.POST)
    @ResponseBody
    public User protected_user_update(Authentication authentication, @RequestBody User inUser) {
        String username = authentication.getName();

        User dbUser = userRepo.findByUsername(username).orElseThrow(NoSuchElementException::new);
        dbUser.setEmail(inUser.getEmail());
        dbUser.setFullName(inUser.getFullName());
        dbUser.setInstitution(inUser.getInstitution());
        log.info(inUser.toString());
        log.info(dbUser.toString());
        // specifically don't let user set their own permissions
        //        dbUser.setPermissions(inUser.getPermissions());
        // there is a dedicated method for updating password
        // String encodedPassword = new BCryptPasswordEncoder().encode(inUser.getPassword());
        // dbUser.setPassword(encodedPassword);
        userRepo.save(dbUser);
        return dbUser;
    }

}

