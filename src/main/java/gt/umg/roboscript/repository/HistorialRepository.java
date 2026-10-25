package gt.umg.roboscript.repository;

import gt.umg.roboscript.entity.HistorialAnalisis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialRepository extends JpaRepository <HistorialAnalisis, Long> {

    List<HistorialAnalisis> findAllByOrderByFechaAnalisisDesc();

}
