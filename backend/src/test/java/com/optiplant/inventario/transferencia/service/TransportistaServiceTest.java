package com.optiplant.inventario.transferencia.service;

import com.optiplant.inventario.common.dto.PageResponse;
import com.optiplant.inventario.common.exception.ValidacionException;
import com.optiplant.inventario.transferencia.dto.TransportistaRequest;
import com.optiplant.inventario.transferencia.dto.TransportistaResponse;
import com.optiplant.inventario.transferencia.entity.Transportista;
import com.optiplant.inventario.transferencia.repository.TransportistaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransportistaServiceTest {

    @Mock
    private TransportistaRepository transportistaRepository;

    @InjectMocks
    private TransportistaService transportistaService;

    @Test
    void crear_nombreRepetido_lanzaValidacionYNoPersiste() {
        when(transportistaRepository.existsByNombreIgnoreCase("servientrega")).thenReturn(true);

        assertThrows(ValidacionException.class,
                () -> transportistaService.crear(new TransportistaRequest("servientrega")));
        verify(transportistaRepository, never()).save(any());
    }

    @Test
    void crear_ok_persisteYDevuelveElNuevoTransportista() {
        when(transportistaRepository.existsByNombreIgnoreCase("Envía S.A.S.")).thenReturn(false);
        when(transportistaRepository.save(any(Transportista.class))).thenAnswer(i -> {
            Transportista t = i.getArgument(0);
            t.setId(5L);
            return t;
        });

        TransportistaResponse response = transportistaService.crear(new TransportistaRequest("Envía S.A.S."));

        assertEquals(5L, response.id());
        assertEquals("Envía S.A.S.", response.nombre());
    }

    @Test
    void listar_devuelveElCatalogoOrdenadoPorNombre() {
        Page<Transportista> pagina = new PageImpl<>(java.util.List.of(
                Transportista.builder().id(1L).nombre("Coordinadora Mercantil").build(),
                Transportista.builder().id(2L).nombre("Servientrega").build()));
        when(transportistaRepository.findAllByOrderByNombreAsc(any())).thenReturn(pagina);

        PageResponse<TransportistaResponse> response = transportistaService.listar(PageRequest.of(0, 20));

        assertEquals(2, response.content().size());
        assertEquals("Coordinadora Mercantil", response.content().get(0).nombre());
    }
}
