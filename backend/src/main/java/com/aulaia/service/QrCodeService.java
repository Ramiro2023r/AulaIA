package com.aulaia.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Generación de imágenes QR (Prompt 4.5, 07-PLAN).
 *
 * <p>Responsabilidad única y acotada: recibe el contenido (texto) y
 * devuelve los bytes PNG del QR. No consulta PostgreSQL, no conoce
 * Usuario ni autorización, no loguea el contenido (privacidad: 04-BD §22).
 *
 * <p>Formato PNG y tamaño 300x300: no están definidos en los documentos;
 * se eligen como decisión técnica mínima estándar (QR limpio negro/blanco,
 * configuración ZXing por defecto, sin logos/colores/marcos).
 */
@Service
public class QrCodeService {

    /** Tamaño en píxeles (lado) del PNG generado. Decisión técnica reportada. */
    private static final int TAMANO_PIXELES = 300;

    /**
     * Genera el PNG del QR con el contenido indicado.
     *
     * @param contenido texto exacto a codificar (para AulaIA:
     *                  {@code AULAIA:STUDENT:<qrToken>}, 07-PLAN 4.5)
     * @return bytes de la imagen PNG
     * @throws IllegalStateException si el contenido no puede codificarse
     */
    public byte[] generarPng(String contenido) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(contenido, BarcodeFormat.QR_CODE, TAMANO_PIXELES, TAMANO_PIXELES);
            BufferedImage imagen = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            ImageIO.write(imagen, "png", salida);
            return salida.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("No se pudo generar la imagen QR", ex);
        }
    }
}