package com.aulaia.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias de {@link QrCodeService} (Prompt 4.5): PNG válido,
 * decodificación exacta del contenido AULAIA:STUDENT:<token> y ausencia de
 * información adicional. Usa el decoder de ZXing sobre los bytes generados.
 */
class QrCodeServiceTest {

    private final QrCodeService qrCodeService = new QrCodeService();

    private String decodificar(byte[] png) throws Exception {
        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(png));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(imagen)));
        return new MultiFormatReader().decode(bitmap).getText();
    }

    @Test
    void contenidoValidoGeneraPngNoVacio() {
        byte[] png = qrCodeService.generarPng("AULAIA:STUDENT:abc123");

        assertThat(png).isNotEmpty();
    }

    @Test
    void bytesTienenFirmaPngValida() {
        byte[] png = qrCodeService.generarPng("AULAIA:STUDENT:abc123");

        assertThat(png).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47);
    }

    @Test
    void dimensionesRazonables() throws Exception {
        byte[] png = qrCodeService.generarPng("AULAIA:STUDENT:abc123");
        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(png));

        assertThat(imagen.getWidth()).isEqualTo(300);
        assertThat(imagen.getHeight()).isEqualTo(300);
        assertThat(png.length).isLessThan(20_000);
    }

    @Test
    void dosLlamadasConMismoContenidoProducenQrEquivalente() throws Exception {
        byte[] png1 = qrCodeService.generarPng("AULAIA:STUDENT:abc123");
        byte[] png2 = qrCodeService.generarPng("AULAIA:STUDENT:abc123");

        assertThat(decodificar(png1)).isEqualTo(decodificar(png2));
        assertThat(decodificar(png1)).isEqualTo("AULAIA:STUDENT:abc123");
    }

    @Test
    void decodificaExactamenteElContenido() throws Exception {
        byte[] png = qrCodeService.generarPng("AULAIA:STUDENT:abc123");

        assertThat(decodificar(png)).isEqualTo("AULAIA:STUDENT:abc123");
    }

    @Test
    void noAgregaInformacionExtraAlContenido() throws Exception {
        String contenido = "AULAIA:STUDENT:abc123";

        byte[] png = qrCodeService.generarPng(contenido);

        assertThat(decodificar(png)).isEqualTo(contenido);
        assertThat(decodificar(png)).isNotEqualTo("abc123");
        assertThat(decodificar(png)).doesNotContain("{", "}", "\"token\"", "abc123:");
    }

    @Test
    void tokenDe64HexSeDecodificaCompleto() throws Exception {
        String token = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        String contenido = "AULAIA:STUDENT:" + token;

        byte[] png = qrCodeService.generarPng(contenido);

        assertThat(decodificar(png)).isEqualTo(contenido);
    }
}