package com.optiplant.inventario.transferencia.entity;

import com.optiplant.inventario.producto.entity.Producto;
import com.optiplant.inventario.sucursal.entity.Sucursal;
import com.optiplant.inventario.transferencia.repository.TransferenciaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Análogo a {@code InventarioSucursalOptimisticLockingTest}: {@link Transferencia}
 * se muta en pasos distintos de su ciclo de vida (solicitar → aprobar → despachar →
 * recibir), típicamente por usuarios de sucursales distintas, por lo que también
 * necesita {@code @Version} para evitar un lost update.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TransferenciaOptimisticLockingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransferenciaRepository transferenciaRepository;

    @Test
    void segundaEscrituraConcurrenteLanzaExcepcionDeBloqueoOptimista() {
        Producto producto = entityManager.persistAndFlush(
                Producto.builder().sku("SKU-1").nombre("Martillo").unidadMedidaBase("UNIDAD").build());
        Sucursal origen = entityManager.persistAndFlush(Sucursal.builder().nombre("Sucursal Centro").build());
        Sucursal destino = entityManager.persistAndFlush(Sucursal.builder().nombre("Sucursal Norte").build());
        Long id = entityManager.persistFlushFind(
                Transferencia.builder()
                        .producto(producto)
                        .sucursalOrigen(origen)
                        .sucursalDestino(destino)
                        .cantidadSolicitada(BigDecimal.TEN)
                        .estado(EstadoTransferencia.PENDIENTE)
                        .urgencia(Urgencia.MEDIA)
                        .build()
        ).getId();

        // Dos "sesiones" cargan la misma fila de forma independiente, ambas en version 0.
        Transferencia instanciaA = transferenciaRepository.findById(id).orElseThrow();
        entityManager.detach(instanciaA);
        Transferencia instanciaB = transferenciaRepository.findById(id).orElseThrow();

        // La sesión B persiste primero su cambio (p. ej. la aprueba): la fila pasa a version 1.
        instanciaB.setEstado(EstadoTransferencia.EN_TRANSITO);
        transferenciaRepository.saveAndFlush(instanciaB);

        // La sesión A, con su copia ya obsoleta (version 0), intenta persistir su propio cambio.
        instanciaA.setEstado(EstadoTransferencia.RECHAZADA);
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> transferenciaRepository.saveAndFlush(instanciaA));
    }
}
