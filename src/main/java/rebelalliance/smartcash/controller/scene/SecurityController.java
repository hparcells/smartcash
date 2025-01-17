package rebelalliance.smartcash.controller.scene;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import rebelalliance.smartcash.controller.BaseController;
import rebelalliance.smartcash.controller.IController;
import rebelalliance.smartcash.database.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

public class SecurityController extends BaseController implements IController {
    @FXML
    protected Button mfaEnableButton;

    @Override
    public void init() {
        this.addHeader();

        this.update();
    }

    @Override
    public void update() {
        // Update UI if user has MFA enabled.
        if(this.sceneManager.getLoggedInUser().hasMfaEnabled()) {
            this.mfaEnableButton.setText("Disable MFA");
        }else {
            this.mfaEnableButton.setText("Enable MFA");
        }
    }

    @FXML
    protected void onMfaButtonClick() {
        User loggedInUser = this.sceneManager.getLoggedInUser();
        if(loggedInUser.hasMfaEnabled()) {
            // Disable MFA.
            this.databaseManager.disableMfa(loggedInUser);

            this.update();
        }else {
            // Enable MFA.
            SecretGenerator secretGenerator = new DefaultSecretGenerator();
            String secret = secretGenerator.generate();
            QrData data = new QrData.Builder()
                    .label(loggedInUser.getEmail())
                    .secret(secret)
                    .issuer("SmartCash")
                    .algorithm(HashingAlgorithm.SHA1)
                    .digits(6)
                    .period(30)
                    .build();
            QrGenerator generator = new ZxingPngQrGenerator();
            String mimeType = generator.getImageMimeType();
            try {
                byte[] imageData = generator.generate(data);
                String dataUri = getDataUriForImage(imageData, mimeType);

                Stage popup;
                Scene scene;
                popup = new Stage();
                popup.setResizable(false);
                popup.setTitle("2FA Setup");

                VBox root = new VBox();
                root.setSpacing(5);
                root.setPadding(new Insets(5));
                root.setAlignment(Pos.TOP_CENTER);
                Text title = new Text("Scan this QR code with your authenticator app. If you can't scan it, you can manually enter the code below.");
                title.setWrappingWidth(400 - 10);
                ImageView qrCode = new ImageView(new Image(dataUri));
                qrCode.maxWidth(300);
                VBox codeBox = new VBox();
                Text secretText = new Text(secret);
                Button copyButton = new Button("Copy");
                copyButton.setOnAction(e -> {
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    ClipboardContent content = new ClipboardContent();
                    content.putString(secret);
                    clipboard.setContent(content);
                });
                codeBox.setAlignment(Pos.CENTER);
                codeBox.getChildren().addAll(secretText, copyButton);
                Text afterScan = new Text("After scanning the QR code, enter the code generated by the authenticator app below.");
                afterScan.setWrappingWidth(400 - 10);
                TextField codeField = new TextField();
                Button submitButton = new Button("Submit");
                submitButton.setOnAction(e -> {
                    TimeProvider timeProvider = new SystemTimeProvider();
                    CodeGenerator codeGenerator = new DefaultCodeGenerator();
                    CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

                    boolean successful = verifier.isValidCode(secret, codeField.getText());

                    if(!successful) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Invalid code.");
                        alert.setContentText("The 2FA code you entered is invalid. Please try again.");
                        alert.showAndWait();
                        return;
                    }

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText("2FA enabled.");
                    alert.setContentText("2FA has been successfully enabled on your account.");
                    alert.showAndWait();

                    // TODO: Store secret in database.
                    // TODO: Update UI to reflect that MFA is enabled.

                    this.databaseManager.enableMfa(loggedInUser, secret);
                    popup.close();
                    this.update();
                });
                root.getChildren().addAll(
                        title,
                        qrCode,
                        codeBox,
                        afterScan,
                        codeField,
                        submitButton
                );

                scene = new Scene(root, 400, 600);
                popup.setScene(scene);
                popup.show();

            }catch(Exception e) {
                e.printStackTrace();
            }
        }
    }


     @FXML
    protected Label personalInfo;

    @FXML
    protected Label accountNumber;
    @FXML
    protected Label routingNumber;
    @FXML
    protected Label accountType;
    @FXML
    protected Label debitNumber;
    @FXML
    protected Label pinNumber;

    @FXML
    protected Label lastLogin;
    @FXML
    protected Label changePass;



    @FXML
    protected VBox containsDetailsButton;


    @FXML
    private Tab accountDetailsTab;

    @FXML
    protected Button viewDetailsButton;


    @FXML
    private void onAccountInfoButtonClick() {


        Dialog<String> dialogPass = new Dialog<>();
        dialogPass.getDialogPane().setMinWidth(400);
        dialogPass.getDialogPane().setMinHeight(200);
        dialogPass.setTitle("Password Verification");
        dialogPass.setHeaderText("Enter Password");

        PasswordField passwordField = new PasswordField();
        dialogPass.getDialogPane().setContent(passwordField);

        ButtonType enterButtonType = new ButtonType("Enter", ButtonBar.ButtonData.OK_DONE);
        dialogPass.getDialogPane().getButtonTypes().setAll(enterButtonType, ButtonType.CANCEL);

        dialogPass.setResultConverter(dialogButton -> {
            if (dialogButton == enterButtonType) {
                return passwordField.getText();
            }
            return null;
        });

        Optional<String> result = dialogPass.showAndWait();

        if (result.isPresent()) {
            try {
                if (isPasswordCorrect(result.get())) {

                    // hide button
                    viewDetailsButton.setVisible(false);

                    // show details
                    personalInfo.setVisible(true);

                    accountNumber.setVisible(true);
                    routingNumber.setVisible(true);
                    accountType.setVisible(true);
                    debitNumber.setVisible(true);
                    pinNumber.setVisible(true);

                    lastLogin.setVisible(true);
                    changePass.setVisible(true);


                } else {
                    accountDetailsTab.setDisable(false);
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Invalid Password");
                    alert.setContentText("The password you entered is incorrect. Please try again.");
                    alert.showAndWait();
                    //accountDetailsTab.setDisable(false);
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

    }


   private boolean isPasswordCorrect(String password) throws NoSuchAlgorithmException {
        String correctPassword = "testing";
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] correctPasswordBytes = correctPassword.getBytes(StandardCharsets.UTF_8);
        md.update(correctPasswordBytes);
        byte[] correctPasswordHash = md.digest();

        byte[] enteredPasswordBytes = password.getBytes(StandardCharsets.UTF_8);
        md.update(enteredPasswordBytes);
        byte[] enteredPasswordHash = md.digest();

        for (int i = 0; i < correctPasswordHash.length; i++) {
            if (correctPasswordHash[i] != enteredPasswordHash[i]) {
                return false;
            }
        }

        return true;
   }
}
