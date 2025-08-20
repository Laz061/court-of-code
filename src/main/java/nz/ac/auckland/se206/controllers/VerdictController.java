package nz.ac.auckland.se206.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Controller for the verdict screen where the user is asked to confirm whether the AI defendant's
 * decision was correct. Provides simple callback hooks so higher‑level game logic can react without
 * tightly coupling to this controller.
 */
public class VerdictController {

  // --- FXML injected controls ---
  @FXML private Button btnYes;
  @FXML private Button btnNo;
  @FXML private Label lblOutcome;

  // Resource paths for outcome texts
  private static final String OUTCOME_RESOURCE = "/prompts/verdict.txt";

  private String cachedYesText;
  private String cachedNoText;

  @FXML
  private void initialize() {
    // Hide outcome label until a choice is made
    if (lblOutcome != null) {
      lblOutcome.setVisible(false);
      lblOutcome.setManaged(false);
    }
  }

  private String loadText(String resourcePath) {
    try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
      if (in == null) {
        return "[Missing resource: " + resourcePath + "]";
      }
      try (BufferedReader br =
          new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
          sb.append(line).append('\n');
        }
        return sb.toString().trim();
      }
    } catch (IOException e) {
      return "[Error reading resource: " + resourcePath + "]";
    }
  }

  private void showOutcome(String text) {
    lblOutcome.setText(text);
    lblOutcome.setManaged(true);
    lblOutcome.setVisible(true);
  }

  /** Handler for Yes button click. */
  @FXML
  private void onBtnYesAction(ActionEvent event) {
    if (cachedYesText == null) {
      cachedYesText = loadText(OUTCOME_RESOURCE);
    }
    showOutcome(cachedYesText);
    btnYes.setDisable(true);
    btnNo.setDisable(true);
  }

  /** Handler for No button click. */
  @FXML
  private void onBtnNoAction(ActionEvent event) {
    if (cachedNoText == null) {
      cachedNoText = loadText(OUTCOME_RESOURCE);
    }
    showOutcome(cachedNoText);
    btnYes.setDisable(true);
    btnNo.setDisable(true);
  }
}
