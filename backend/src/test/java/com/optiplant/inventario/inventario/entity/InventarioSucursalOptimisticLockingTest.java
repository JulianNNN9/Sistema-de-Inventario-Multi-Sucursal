package com.optiplant.inventario.inventario.entity;

import com.optiplant.inventario.inventario.repository.InventarioSucursalRepository;
import com.optiplant.inventario.producto.entity.Producto;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica que la columna {@code version} (@Version, control de concurrencia
 * optimista) evita un lost update en {@link InventarioSucursal}: dos
 * "sesiones" cargan la misma fila, la primera persiste su cambio y la
 * segunda, con su copia ya desactualizada, debe fallar en vez de
 * sobrescribir el cambio de la primera en silencio.
 *
 * <p>Usa un H2 embebido con el esquema generado directamente desde las
 * anotaciones JPA (Flyway deshabilitado solo para este test) para no
 * depender de Docker/Postgres al correr {@code mvn test}.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class InventarioSucursalOptimisticLockingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InventarioSucursalRepository inventarioSucursalRepository;

    @Test
    void segundaEscrituraConcurrenteLanzaExcepcionDeBloqueoOptimista() {
        Producto producto = entityManager.persistAndFlush(
                Producto.builder().sku("SKU-1").nombre("Martillo").unidadMedidaBase("UNIDAD").build());
        Sucursal sucursal = entityManager.persistAndFlush(
                Sucursal.builder().nombre("Sucursal Centro").build());
        Long id = entityManager.persistFlushFind(
                InventarioSucursal.builder()
                        .producto(producto)
                        .sucursal(sucursal)
                        .cantidadActual(BigDecimal.TEN)
                        .stockMinimo(BigDecimal.ONE)
                        .costoPromedioPonderado(BigDecimal.valueOf(5))
                        .build()
        ).getId();

        // Dos "sesiones" cargan la misma fila de forma independiente, ambas en version 0.
        InventarioSucursal instanciaA = inventarioSucursalRepository.findById(id).orElseThrow();
        entityManager.detach(instanciaA);
        InventarioSucursal instanciaB = inventarioSucursalRepository.findById(id).orElseThrow();

        // La sesión B persiste primero su cambio: la fila pasa a version 1.
        instanciaB.setCantidadActual(instanciaB.getCantidadActual().subtract(BigDecimal.ONE));
        inventarioSucursalRepository.saveAndFlush(instanciaB);

        // La sesión A, con su copia ya obsoleta (version 0), intenta persistir su propio cambio.
        instanciaA.setCantidadActual(instanciaA.getCantidadActual().subtract(BigDecimal.ONE));
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> inventarioSucursalRepository.saveAndFlush(instanciaA));
    }
}
