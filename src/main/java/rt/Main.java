package rt;

import rt.core.Core;

public class Main {

    public static void main(String[] args) {
        Core core = new Core();
        core.start();

//        var api = new ExternalAPIHandler();
//        Agent agent = new Agent(api, new SQLiteDB(), new EmbeddingClient(api));
//
//        try (Scanner scanner = new Scanner(System.in)) {
//            while (true) {
//                System.out.print("> ");
//                String input = scanner.nextLine();
//                if (input.equalsIgnoreCase("stop")) break;
//                String answer = agent.ask(input);
//                System.out.println(answer);
//            }
//        } catch (IOException | InterruptedException e) {
//            throw new RuntimeException(e);
//        }
    }
}