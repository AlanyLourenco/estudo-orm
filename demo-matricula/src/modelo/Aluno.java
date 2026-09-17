package modelo;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A entidade principal da demonstração: vira a tabela "aluno". */
@Entity
@Table(name = "aluno")
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(unique = true)
    private String email;

    // N:1 — vira a coluna aluno.turma_id (Aluno é o dono da FK).
    // ...ToOne é EAGER por padrão: a turma vem junto, com JOIN.
    @ManyToOne
    @JoinColumn(name = "turma_id")
    private Turma turma;

    // N:N — vira a tabela de junção aluno_disciplina.
    @ManyToMany
    @JoinTable(name = "aluno_disciplina",
               joinColumns = @JoinColumn(name = "aluno_id"),
               inverseJoinColumns = @JoinColumn(name = "disciplina_id"))
    private Set<Disciplina> disciplinas = new HashSet<>();

    protected Aluno() { } // exigido pela JPA

    public Aluno(String nome, String email) {
        this.nome = nome;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public Turma getTurma() { return turma; }
    public void setTurma(Turma turma) { this.turma = turma; }
    public Set<Disciplina> getDisciplinas() { return disciplinas; }
}
