package rt.ai;

public final class Constants {

    private Constants() {
    }

    static final int MAX_MESSAGES = 50;
    static final int DISTRIBUTION_LIMIT = 50;

    static final String INITIAL_SYSTEM_MESSAGE = """
            You answer questions based on a Russian text database.
            Use the database tools whenever the answer requires information from the database.
            Do not invent facts that are not supported by the database or conversation.
            Choose the database tool according to the user's request:
            - semantic_search:
              Use for meaning, context, facts, events, topics, or other information that should be found by semantic similarity.
            - exact_search:
              Use when the user asks to find a specific literal word, name, phrase, term, code, or other exact text occurrence.
            - last_messages_search:
              Use when the user asks for the latest messages in the database.
            - get_database_stats:
              Use when the user asks about the database as a whole, its size, structure, statistics, date range, or aggregated topic/entity information.
            If a request requires several kinds of information, use multiple tools when necessary.
            Never use search tools to retrieve the entire database.
            If the requested analysis requires more data than can reasonably be retrieved, ask the user to narrow the request by topic, time period, chat, or another relevant criterion.
            You have only %d tool calls available for one user's request.
            Use the available calls efficiently.
            Analyze the retrieved information and answer the user's original question.
            If the required information cannot be found in the database, say so clearly.
            """;

    static final String SEMANTIC_SEARCH_TOOL_NAME = "semantic_search";
    static final String SEMANTIC_SEARCH_TOOL_DESC = """
            Search messages stored in the local database by semantic similarity.
            Provide a detailed natural-language query describing the information you want to find.
            Include relevant context, names, terms, events, actions, dates, and other useful details when available.
            More detailed queries generally improve semantic search quality.
            Semantic search is available only for Russian-language content.
            The tool returns up to %d matching messages with their link, chat name, text, and date of publishing.
            """;
    static final String SEMANTIC_SEARCH_TOOL_PARAMS = """
            {
                  "type": "object",
                  "properties": {
                    "query": {
                      "type": "string",
                      "description": "A detailed and specific natural-language description of the information to find. Include relevant context, names, terms, events, actions, dates, or other details when available. More detailed queries improve semantic search quality."
                    }
                  },
                  "required": ["query"]
                }
            """;


    static final String EXACT_SEARCH_TOOL_NAME = "exact_search";
    static final String EXACT_SEARCH_TOOL_DESC = """
            Search messages by exact text occurrence.
            The search is case-insensitive and treats the query as literal text, not as a regular expression.
            The tool returns up to %d matching messages with their link, chat name, text, and date of publishing.
            """;
    static final String EXACT_SEARCH_TOOL_PARAMS = """
            {
                  "type": "object",
                  "properties": {
                    "query": {
                      "type": "string",
                      "description": "The exact word, name, phrase, term, code, or other literal text to find."
                    }
                  },
                  "required": ["query"]
                }
            """;
    static final String LAST_SEARCH_TOOL_NAME = "last_messages_search";
    static final String LAST_SEARCH_TOOL_DESC = """
            Search the newest messages in the database.
            The tool returns up to %d matching messages with their link, chat name, text, and date of publishing.
            """;
    static final String LAST_SEARCH_TOOL_PARAMS = """
            {
                  "type": "object",
                  "properties": {}
                }
            """;

    static final String DATABASE_STATS_TOOL_NAME = "get_database_stats";
    static final String DATABASE_STATS_TOOL_DESC = """
            Get aggregated statistics about the message database without loading individual message contents.
            The tool returns:
            - total number of messages;
            - total number of chats;
            - total number of named entities;
            - total number of topics;
            - number of active days;
            - date range of stored messages;
            - top %d topics by number of associated messages;
            - top %d named entities by number of associated messages.
            A message can belong to multiple topics and contain multiple named entities.
            Therefore, one message can be counted in several groups, and the sum of topic/entity counts can exceed the total number of messages.
            The tool does not return message contents.
            """;
    static final String DATABASE_STATS_TOOL_PARAMS = """
            {
                  "type": "object",
                  "properties": {}
                }
            """;
    static final String DATABASE_STATS_TOOL_RESULT = """
            Database statistics:
                Total messages: %d
                Total chats: %d
                Total named entities: %d
                Total topics: %d
                Active days: %d
                First message: %s
                Last message: %s
            
                Top %d topics by number of associated messages:
                %s
            
                Top %d named entities by number of associated messages:
                %s
            
                Note: a message can belong to multiple topics and contain multiple named entities.
                Therefore, one message can be counted in several groups, and the sum of topic/entity counts can exceed the total number of messages.
            """;
}
