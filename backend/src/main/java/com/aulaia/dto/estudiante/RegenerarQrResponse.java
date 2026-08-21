package com.aulaia.dto.estudiante;

/**
 * Respuesta mínima de regeneración de QR (Prompt 4.4).
 *
 * <p>El qrToken nunca se expone en respuestas (privacidad: 04-BD §22/§6.5);
 * el contrato de refresco del QR del administrador usa el endpoint de
 * imagen del Prompt 4.5 (GET /api/v1/estudiantes/{id}/qr). Ningún documento
 * define un response para la regeneración; se elige el contrato mínimo
 * {@code {"success": true}} (decisión técnica reportada).
 */
public record RegenerarQrResponse(boolean success) {
}