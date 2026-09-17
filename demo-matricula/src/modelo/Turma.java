package modelo;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/** Lado "um" do relacionamento 1:N com Aluno. */
@Entity
@Table(name = "turma")
public class Turma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String nome;

    // mappedBy: a chave estrangeira fica em Aluno.turma (Aluno é o dono).
    // Coleções são LAZY por padrão: os alunos só são lidos quando alguém pedir.
    @OneToMany(mappedBy = "turma")
    private List<Aluno> alunos = new ArrayList<>();

    protected Turma() { } // exigido pela JPA

    public Turma(String nome) {
        this.nome = nome;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public List<Aluno> getAlunos() { return alunos; }
}
