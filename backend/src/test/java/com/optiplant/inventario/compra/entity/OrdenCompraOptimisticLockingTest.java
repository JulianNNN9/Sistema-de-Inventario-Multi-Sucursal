package com.optiplant.inventario.compra.entity;

import com.optiplant.inventario.compra.repository.OrdenCompraRepository;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Análogo a {@code InventarioSucursalOptimisticLockingTest}: {@link OrdenCompra}
 * se muta al confirmar su recepción (potencialmente por un actor distinto del que
 * la creó), por lo que también necesita {@code @Version} para evitar un lost
 * update (p. ej. dos confirmaciones concurrentes duplicando el ingreso a inventario).
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class OrdenCompraOptimisticLockingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrdenCompraRepository ordenCompraRepository;

    @Test
    void segundaEscrituraConcurrenteLanzaExcepcionDeBloqueoOptimista() {
        Proveedor proveedor = entityManager.persistAndFlush(
                Proveedor.builder().nombre("Ferretería Central").frecuenciaPago("30 días").build());
        Sucursal sucursal = entityManager.persistAndFlush(Sucursal.builder().nombre("Sucursal Centro").build());
        Long id = entityManager.persistFlushFind(
                OrdenCompra.builder()
                        .proveedor(proveedor)
                        .sucursal(sucursal)
                        .fecha(Instant.now())
                        .estado(EstadoOrdenCompra.PENDIENTE)
                        .build()
        ).getId();

        // Dos "sesiones" cargan la misma fila de forma independiente, ambas en version 0.
        OrdenCompra instanciaA = ordenCompraRepository.findById(id).orElseThrow();
        entityManager.detach(instanciaA);
        OrdenCompra instanciaB = ordenCompraRepository.findById(id).orElseThrow();

        // La sesión B confirma primero la recepción: la fila pasa a version 1.
        instanciaB.setEstado(EstadoOrdenCompra.RECIBIDA);
        ordenCompraRepository.saveAndFlush(instanciaB);

        // La sesión A, con su copia ya obsoleta (version 0), intenta confirmar la misma recepción.
        instanciaA.setEstado(EstadoOrdenCompra.RECIBIDA);
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> ordenCompraRepository.saveAndFlush(instanciaA));
    }
}
