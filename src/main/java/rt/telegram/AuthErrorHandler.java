package rt.telegram;

import it.tdlight.client.GenericResultHandler;
import it.tdlight.client.Result;
import it.tdlight.jni.TdApi;

import javax.swing.*;

class AuthErrorHandler implements GenericResultHandler<TdApi.Ok> {

    @Override
    public void onResult(Result<TdApi.Ok> result) {
        if (result.getError() != null) {
            showAuthErrorMessage("Произошла ошибка при авторизации: " + result.getError() + System.lineSeparator() + "Требуется перезапуск");
            System.exit(0);
        }
    }

    private void showAuthErrorMessage(String errorText) {
        JOptionPane.showMessageDialog(
                null,
                errorText,
                "Ошибка",
                JOptionPane.ERROR_MESSAGE
        );
    }
}