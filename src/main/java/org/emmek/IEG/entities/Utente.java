package org.emmek.IEG.entities;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;


@Getter
@Setter
@Entity
@Table(name = "utenti")
@NoArgsConstructor
@JsonIgnoreProperties({"password", "authorities", "enabled", "credentialsNonExpired", "accountNonExpired", "accountNonLocked"})
public class Utente implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String username;
    private String nome;
    @Column(unique = true)
    private String email;
    private String cognome;
    private String urlAvatar;
    private String password;
    @ManyToMany(mappedBy = "utenti", fetch = FetchType.EAGER)
    private Set<Ruolo> ruoli;

    @CreationTimestamp
    private Date createdAt;

    public Utente(String basicUser, String mail, String password) {
        this.username = basicUser;
        this.email = mail;
        this.password = password;
    }

    @Override

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return ruoli.stream()
                .map(ruolo -> new SimpleGrantedAuthority(ruolo.getRuolo()))
                .collect(Collectors.toList());
    }


    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }


    public void addRuolo(Ruolo ruolo) {
        if (this.ruoli == null) {
            this.ruoli = new HashSet<>();
        }
        this.ruoli.add(ruolo);
    }
}
