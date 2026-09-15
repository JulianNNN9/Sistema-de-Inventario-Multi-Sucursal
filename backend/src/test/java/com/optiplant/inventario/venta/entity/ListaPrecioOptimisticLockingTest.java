package com.optiplant.inventario.venta.entity;

import com.optiplant.inventario.venta.repository.ListaPrecioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Análogo a {@code TransferenciaOptimisticLockingTest} / {@code OrdenCompraOptimisticLockingTest}:
 * {@link ListaPrecio#actualizar} borra y reinserta los ítems en cada edición, así
 * que sin {@code @Version} dos administradores editando la misma lista casi al
 * mismo tiempo producirían un lost update silencioso.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ListaPrecioOptimisticLockingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ListaPrecioRepository listaPrecioRepository;

    @Test
    void segundaEscrituraConcurrenteLanzaExcepcionDeBloqueoOptimista() {
        Long id = entityManager.persistFlushFind(
                ListaPrecio.builder().nombre("Lista general").build()
        ).getId();

        // Dos "sesiones" cargan la misma fila de forma independiente, ambas en version 0.
        ListaPrecio instanciaA = listaPrecioRepository.findById(id).orElseThrow();
        entityManager.detach(instanciaA);
        ListaPrecio instanciaB = listaPrecioRepository.findById(id).orElseThrow();

        // La sesión B persiste primero su cambio: la fila pasa a version 1.
        instanciaB.setNombre("Lista general (editada por B)");
        listaPrecioRepository.saveAndFlush(instanciaB);

        // La sesión A, con su copia ya obsoleta (version 0), intenta persistir su propio cambio.
        instanciaA.setNombre("Lista general (editada por A)");
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> listaPrecioRepository.saveAndFlush(instanciaA));
    }
}
