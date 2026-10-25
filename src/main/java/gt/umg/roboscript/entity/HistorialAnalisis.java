package gt.umg.roboscript.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_analisis")
@Getter
@Setter
@NoArgsConstructor
public class HistorialAnalisis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombreArchivo;

    @Column(nullable = false)
    private LocalDateTime fechaAnalisis;

    private boolean valido;
    private int tokensReconocidos;
    private int erroresLexicos;
    private int erroresSintacticos;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String codigoFuente;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String reporte;
}
