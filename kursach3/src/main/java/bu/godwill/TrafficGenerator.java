package bu.godwill;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Класс, генерирующий трафик и обрабатывающий подключения клиентов.
 */
public class TrafficGenerator {
    // Счетчик для синхронизации между сервером и клиентом
    public static CountDownLatch latch = new CountDownLatch(1);

    /**
     * Точка входа в серверный компонент приложения.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        latch = new CountDownLatch(1); // Инициализируем счетчик
        int port = 12345; // Порт для подключения клиента

        // Получаем ввод пользователя для настройки генерации трафика
        int[] userInput = getUserInput();
        int packetCount = userInput[0]; // Количество пакетов
        int packetSize = userInput[1];   // Размер пакета
        int frequency = userInput[2];     // Частота отправки пакетов

        // Запускаем серверный сокет для ожидания подключения клиента
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен. Ожидание подключения клиента...");
            latch.countDown(); // Уведомляем, что сервер готов

            // Ожидаем подключения клиента
            try (Socket clientSocket = serverSocket.accept()) {
                System.out.println("Клиент подключен.");
                // Обрабатываем подключение клиента
                processClientConnection(clientSocket, packetCount, packetSize, frequency);
            } catch (IOException e) {
                System.err.println("Ошибка при подключении клиента: " + e.getMessage());
            }
        } catch (IOException e) {
            handleError(e); // Обрабатываем ошибки ввода-вывода
        }
    }

    /**
     * Получает ввод пользователя для настройки генерации трафика.
     *
     * @return массив с количеством пакетов, размером пакета и частотой отправки
     */
    private static int[] getUserInput() {
        Scanner scanner = new Scanner(System.in); // Создаем сканер для ввода
        // Запрашиваем у пользователя количество пакетов, размер пакета и частоту отправки
        int packetCount = getValidatedInput(scanner, "Введите количество пакетов (макс. 50000): ", 1, 50000);
        int packetSize = getValidatedInput(scanner, "Введите объем пакета (в байтах, макс. 1000000): ", 1, 1000000);
        int frequency = getValidatedInput(scanner, "Введите частоту отправки (в миллисекундах, макс. 10000000): ", 0, 10000000);
        scanner.close(); // Закрываем сканер
        return new int[]{packetCount, packetSize, frequency}; // Возвращаем массив с параметрами
    }

    /**
     * Проверяет и валидирует ввод пользователя.
     *
     * @param scanner объект Scanner для чтения ввода
     * @param prompt  сообщение для пользователя
     * @param min     минимальное допустимое значение
     * @param max     максимальное допустимое значение
     * @return валидированное значение
     */
    private static int getValidatedInput(Scanner scanner, String prompt, int min, int max) {
        int value = 0; // Переменная для хранения валидированного значения
        while (true) {
            System.out.print(prompt); // Выводим подсказку пользователю
            if (scanner.hasNextInt()) { // Проверяем, ввел ли пользователь целое число
                value = scanner.nextInt(); // Считываем введенное значение
                // Проверяем, входит ли значение в допустимый диапазон
                if (value >= min && value <= max) {
                    break; // Если значение валидно, выходим из цикла
                } else {
                    System.out.println("Ошибка: значение должно быть от " + min + " до " + max + ".");
                }
            } else {
                System.out.println("Ошибка: введите целое число."); // Сообщаем об ошибке
                scanner.next(); // Пропускаем некорректный ввод
            }
        }
        return value; // Возвращаем валидированное значение
    }

    /**
     * Обрабатывает подключение клиента и отправляет пакеты.
     *
     * @param clientSocket сокет клиента
     * @param packetCount  количество пакетов для отправки
     * @param packetSize   размер пакета
     * @param frequency    частота отправки пакетов
     */
    private static void processClientConnection(Socket clientSocket, int packetCount, int packetSize, int frequency) {
        try (OutputStream out = clientSocket.getOutputStream();
             DataOutputStream dataOut = new DataOutputStream(out)) {
            // Отправляем начальные данные клиенту
            sendInitialData(dataOut, packetCount, packetSize);
            // Отправляем пакеты и получаем время, затраченное на отправку
            long duration = sendPackets(packetCount, packetSize, out, frequency);
            // Отображаем сообщение о количестве отправленных пакетов и времени
            displayMessage(packetCount, duration);
        } catch (IOException e) {
            System.err.println("Ошибка при взаимодействии с клиентом: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
            System.err.println("Ошибка при ожидании: " + e.getMessage());
        } finally {
            try {
                clientSocket.close(); // Закрываем сокет клиента
            } catch (IOException e) {
                System.err.println("Ошибка при закрытии сокета клиента: " + e.getMessage());
            }
        }
    }

    /**
     * Отправляет начальные данные клиенту.
     *
     * @param dataOut     поток для отправки данных
     * @param packetCount количество пакетов
     * @param packetSize  размер пакета
     * @throws IOException если произошла ошибка ввода-вывода
     */
    private static void sendInitialData(DataOutputStream dataOut, int packetCount, int packetSize) throws IOException {
        dataOut.writeInt(packetCount); // Отправляем количество пакетов
        dataOut.writeInt(packetSize);   // Отправляем размер пакета
    }

    /**
     * Отправляет пакеты клиенту.
     *
     * @param packetCount количество пакетов
     * @param packetSize  размер пакета
     * @param out        поток для отправки данных
     * @param frequency   частота отправки пакетов
     * @return время, затраченное на отправку пакетов
     * @throws IOException если произошла ошибка ввода-вывода
     * @throws InterruptedException если поток был прерван
     */
    private static long sendPackets(int packetCount, int packetSize, OutputStream out, int frequency) throws IOException, InterruptedException {
        long startTime = System.nanoTime(); // Запоминаем время начала отправки
        byte[] packet = new byte[packetSize]; // Создаем массив для пакета

        // Цикл для отправки заданного количества пакетов
        for (int i = 1; i <= packetCount; i++) {
            sendPacket(out, packet, i); // Отправляем пакет
            TimeUnit.MILLISECONDS.sleep(frequency); // Ждем перед отправкой следующего пакета
        }

        return System.nanoTime() - startTime; // Возвращаем время, затраченное на отправку
    }

    /**
     * Отправляет один пакет клиенту.
     *
     * @param out          поток для отправки данных
     * @param packet       массив байтов пакета
     * @param packetNumber номер пакета
     * @throws IOException если произошла ошибка ввода-вывода
     */
    private static void sendPacket(OutputStream out, byte[] packet, int packetNumber) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream dataStream = new DataOutputStream(byteStream)) {
            dataStream.writeInt(packetNumber); // Записываем номер пакета
            dataStream.writeLong(System.nanoTime()); // Записываем метку времени
            dataStream.write(packet); // Записы dataStream.write(packet); // Записываем данные пакета
            out.write(byteStream.toByteArray()); // Отправляем пакет клиенту
            out.flush(); // Очищаем поток
        }
    }

    /**
     * Отображает сообщение о количестве отправленных пакетов и времени.
     *
     * @param packetCount количество отправленных пакетов
     * @param duration    время, затраченное на отправку
     */
    private static void displayMessage(int packetCount, long duration) {
        System.out.println("Отправлено " + packetCount + " пакетов за " + duration + " нс."); // Выводим информацию о отправленных пакетах
    }

    /**
     * Обрабатывает ошибки ввода-вывода.
     *
     * @param e исключение ввода-вывода
     */
    private static void handleError(IOException e) {
        System.err.println("Ошибка: " + e.getMessage()); // Выводим сообщение об ошибке
    }
}