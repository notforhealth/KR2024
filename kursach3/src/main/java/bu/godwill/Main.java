package bu.godwill;

/**
 * Главный класс приложения, который запускает сервер и клиент.
 */
public class Main {
    /**
     * Точка входа в приложение.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        // Создаем новый поток для запуска сервера
        Thread serverThread = new Thread(() -> {
            try {
                TrafficGenerator.main(args); // Запускаем генератор трафика
            } catch (Exception e) {
                System.err.println("Ошибка в сервере: " + e.getMessage());
            }
        });
        serverThread.start(); // Запускаем поток сервера

        // Запускаем клиентское приложение
        try {
            TrafficClient.main(args);
        } catch (Exception e) {
            System.err.println("Ошибка в клиенте: " + e.getMessage());
        }
    }
}

