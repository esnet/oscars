package net.es.oscars.security.ent;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "tbl_users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements Serializable, UserDetails {
    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    @NonNull
    @Column(unique = true)
    @Builder.Default
    private String username = "default";

    @NonNull
    @Column
    @Builder.Default
    @JsonIgnore
    private String password = "";

    @Column
    private String fullName;

    @Column
    private String email;

    @Column
    private String institution;

    @Embedded
    private Permissions permissions;


    @Override
    @JsonIgnore
    public Set<GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> res = new HashSet<>();
        if (this.permissions.isAdminAllowed()) {
            res.add(new SimpleGrantedAuthority("ADMIN"));
        }
        res.add(new SimpleGrantedAuthority("USER"));
        return res;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;

    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;

    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return true;

    }
}
