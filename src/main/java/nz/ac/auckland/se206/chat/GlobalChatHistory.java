package nz.ac.auckland.se206.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;

/**
 * GlobalChatHistory stores a shared cross-controller conversation history. It reuses the existing
 * {@link ChatMessage} class. To distinguish which controller/persona produced a message we encode
 * the role in the form "<conversationId>:<baseRole>" where baseRole is one of
 * system/user/assistant.
 *
 * <p>Controllers can then build a summarised context string (e.g. via buildExternalContext) that
 * they inject as a system message ahead of their own user input.
 */
public class GlobalChatHistory {

  private static final GlobalChatHistory INSTANCE = new GlobalChatHistory();
  private static final int MAX_HISTORY = 500; // simple cap

  private GlobalChatHistory() {}

  public static GlobalChatHistory getInstance() {
    return INSTANCE;
  }

  private final List<ChatMessage> history = new ArrayList<>();

  /** Adds a namespaced message to global history. */
  public synchronized void add(String conversationId, String baseRole, String content) {
    history.add(new ChatMessage(conversationId + ":" + baseRole, content));
    if (history.size() > MAX_HISTORY) {
      history.remove(0);
    }
  }

  /** Snapshot of all messages. */
  public synchronized List<ChatMessage> getAll() {
    return Collections.unmodifiableList(new ArrayList<>(history));
  }

  /**
   * Builds a concise external context string excluding the given conversation's own entries. Limits
   * to maxEntries most recent (after filtering) to control token usage.
   */
  public synchronized String buildExternalContext(String conversationId, int maxEntries) {
    String excludePrefix = conversationId + ":";
    // goes through all chat history and appends the ones that are not from the caller
    List<ChatMessage> filtered = new ArrayList<>();
    for (int i = history.size() - 1; i >= 0 && filtered.size() < maxEntries; i--) {
      ChatMessage m = history.get(i);
      if (!m.getRole().startsWith(excludePrefix)) {
        filtered.add(0, m); // prepend to keep chronological order
      }
    }
    if (filtered.isEmpty()) {
      return "No prior external conversations.";
    }

    // builds the chat history and returns
    StringBuilder sb = new StringBuilder();
    sb.append("Relevant prior dialogues from other roles (truncated):\n");
    for (ChatMessage m : filtered) {
      String role = m.getRole();
      int idx = role.indexOf(':');
      String convo = idx > 0 ? role.substring(0, idx) : role;
      String base = idx > 0 ? role.substring(idx + 1) : "";
      sb.append('[')
          .append(convo)
          .append(' ')
          .append(base)
          .append("] ")
          .append(m.getContent())
          .append('\n');
    }
    return sb.toString();
  }
}
