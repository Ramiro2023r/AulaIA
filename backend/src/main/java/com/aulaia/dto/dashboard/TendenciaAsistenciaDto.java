package com.aulaia.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TendenciaAsistenciaDto {
    private LocalDate fecha;
    private double porcentajeAsistencia;
}
