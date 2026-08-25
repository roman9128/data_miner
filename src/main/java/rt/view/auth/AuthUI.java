package rt.view.auth;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class AuthUI {

    private final AuthQrCodeWindow qrCodeWindow = new AuthQrCodeWindow();

    public void showQrCode(String link) {
        qrCodeWindow.showQRCode(link);
    }

    public void closeAuthWindow() {
        qrCodeWindow.dispose();
    }

    private class AuthQrCodeWindow extends JFrame {

        public AuthQrCodeWindow() {
            setTitle("Авторизация");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(400, 450);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());
            setVisible(false);
        }

        private void showQRCode(String url) {
            try {
                BitMatrix matrix = new MultiFormatWriter().encode(
                        url,
                        BarcodeFormat.QR_CODE,
                        300,
                        300
                );

                BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(matrix);
                ImageIcon icon = new ImageIcon(qrImage);

                JLabel qrLabel = new JLabel(icon, SwingConstants.CENTER);
                qrLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

                JLabel manualLabel = new JLabel("Настройки - Устройства - Подключить устройство", SwingConstants.CENTER);
                manualLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
                manualLabel.setFont(new Font("Noto Sans", Font.BOLD, 10));

                add(qrLabel, BorderLayout.CENTER);
                add(manualLabel, BorderLayout.SOUTH);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Ошибка генерации QR кода: " + e.getMessage(),
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
                dispose();
            }
            setVisible(true);
        }
    }
}