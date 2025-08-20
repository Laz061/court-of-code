package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest.Model;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.Choice;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.se206.chat.GlobalChatHistory; // added
import nz.ac.auckland.se206.prompts.PromptEngineering;

public class SecurityController {

  @FXML private ImageView backgroundImage;
  @FXML private TextArea chatBox;
  @FXML private TextField chatInput;
  @FXML private Button btnSend;
  @FXML private Label lblThink;

  private ChatCompletionRequest chatCompletionRequest;
  private Task<ChatMessage> apiTask;
  private String role;
  private final String conversationId = "Sentinel Unit";

  /**
   * Initializes the room view. If it's the first time initialization, it will provide instructions
   * via text-to-speech.
   */
  @FXML
  public void initialize() {
    setRole();
    if (lblThink != null) {
      lblThink.setVisible(false);
      lblThink.setManaged(false);
    }
  }

  /**
   * Generates the system prompt for the current role.
   *
   * @return the system prompt string
   */
  private String getSystemPrompt() {
    try {
      URL resourceUrl =
          PromptEngineering.class.getClassLoader().getResource("prompts/security.txt");
      role = PromptEngineering.loadTemplate(resourceUrl.toURI());
    } catch (IOException | URISyntaxException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Error loading or filling the prompt template.", e);
    }

    Map<String, String> map = new HashMap<>();
    map.put("role", role);

    return PromptEngineering.getPrompt("context.txt", map);
  }

  /**
   * Sets the profession for the chat context and initializes the ChatCompletionRequest.
   *
   * @param profession the profession to set
   */
  public void setRole() {
    try {
      ApiProxyConfig config = ApiProxyConfig.readConfig();
      chatCompletionRequest =
          new ChatCompletionRequest(config)
              .setN(1)
              .setTemperature(0.2)
              .setTopP(0.5)
              .setModel(Model.GPT_4_1_MINI)
              .setMaxTokens(500);
      chatCompletionRequest.addMessage(new ChatMessage("system", getSystemPrompt()));
      // record system prompt to global history (namespaced role stored internally)
      GlobalChatHistory.getInstance().add(conversationId, "system", "(persona prompt initialised)");
    } catch (ApiProxyException e) {
      e.printStackTrace();
    }
  }

  /**
   * Appends a chat message to the chat text area.
   *
   * @param msg the chat message to append
   */
  private void appendChatMessage(ChatMessage msg) {
    String displayRole = msg.getRole();
    if ("assistant".equals(displayRole)) {
      displayRole = conversationId; // show persona label instead of generic assistant
    }
    chatBox.appendText(displayRole + ": " + msg.getContent() + "\n\n");
  }

  /**
   * Runs the GPT model with a given chat message.
   *
   * @param msg the chat message to process
   * @return the response chat message
   * @throws ApiProxyException if there is an errr communicating with the API proxy
   */
  private ChatMessage runGpt(ChatMessage msg) throws ApiProxyException {
    // Before executing, inject a dynamic system message with external context (other roles)
    String externalContext =
        GlobalChatHistory.getInstance().buildExternalContext(conversationId, 25);

    chatCompletionRequest.addMessage(new ChatMessage("system", externalContext));
    chatCompletionRequest.addMessage(msg);

    try {
      ChatCompletionResult chatCompletionResult = chatCompletionRequest.execute();
      Choice result = chatCompletionResult.getChoices().iterator().next();
      ChatMessage assistantMessage = result.getChatMessage();
      chatCompletionRequest.addMessage(assistantMessage);

      // record assistant reply
      GlobalChatHistory.getInstance()
          .add(conversationId, assistantMessage.getRole(), assistantMessage.getContent());

      return assistantMessage;
    } catch (ApiProxyException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Sends a message to the GPT model.
   *
   * @param event the action event triggered by the send button
   * @throws ApiProxyException if there is an error communicating with the API proxy
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void onBtnSendAction(ActionEvent event) throws ApiProxyException, IOException {
    String message = chatInput.getText().trim();
    if (message.isEmpty()) {
      return;
    }

    chatInput.clear();
    btnSend.setDisable(true);
    chatInput.setDisable(true);
    if (lblThink != null) {
      lblThink.setManaged(true);
      lblThink.setVisible(true);
    }

    ChatMessage msg = new ChatMessage("user", message);
    appendChatMessage(msg);
    GlobalChatHistory.getInstance().add(conversationId, msg.getRole(), msg.getContent());

    apiTask =
        new Task<ChatMessage>() {
          @Override
          protected ChatMessage call() throws Exception {
            return runGpt(msg); // background thread
          }
        };

    apiTask.setOnSucceeded(
        e -> {
          ChatMessage result = apiTask.getValue();
          if (result != null) {
            appendChatMessage(result);
          }
          btnSend.setDisable(false);
          chatInput.setDisable(false);
          if (lblThink != null) {
            lblThink.setVisible(false);
            lblThink.setManaged(false);
          }
        });

    apiTask.setOnFailed(
        e -> {
          Throwable ex = apiTask.getException();
          if (ex != null) {
            ex.printStackTrace();
          }
          btnSend.setDisable(false);
          chatInput.setDisable(false);
          if (lblThink != null) {
            lblThink.setVisible(false);
            lblThink.setManaged(false);
          }
        });

    new Thread(apiTask, "security-chat-task").start(); // was patrol-chat-task
  }
}
