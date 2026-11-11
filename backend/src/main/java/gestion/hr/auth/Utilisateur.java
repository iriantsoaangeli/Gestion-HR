package gestion.hr.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Compte de l'application : table {@code utilisateur(id, login, mdp, role)} de base/init.sql.
 * {@code mdp} contient le hachage BCrypt, jamais le mot de passe en clair.
 */
@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String login;

    @Column(nullable = false, length = 100)
    private String mdp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    protected Utilisateur() {
        // pour JPA
    }

    public Utilisateur(Integer id, String login, String mdp, Role role) {
        this.id = id;
        this.login = login;
        this.mdp = mdp;
        this.role = role;
    }

    public Integer getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getMdp() {
        return mdp;
    }

    public Role getRole() {
        return role;
    }
}
