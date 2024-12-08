package bu.godwill;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/**
 * Класс клиента, который подключается к серверу и принимает пакеты.
 */
public class TrafficClient {
    /**
     * Точка входа в клиентский компонент приложения.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        String serverAddress = "localhost"; // Адрес сервера
        int port = 12345; // Порт сервера

        try {
            TrafficGenerator.latch.await(); // Ожидаем, пока сервер будет готов
            try (Socket socket = new Socket(serverAddress, port)) { // Подключаемся к серверу
                if (socket.isConnected()) {
                    System.out.println("Подключение установлено. Прием пакетов...");
                    processPackets(socket); // Обрабатываем пакеты
                }
            } catch (IOException e) {
                System.err.println("Ошибка: " + e.getMessage()); // Обрабатываем ошибки подключения
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            System.err.println("Ошибка ожидания: " + e.getMessage()); // Обрабатываем ошибки ожидания
        }
    }

    /**
     * Обрабатывает получение пакетов от сервера.
     *
     * @param socket сокет для получения данных
     * @throws IOException если произошла ошибка ввода-вывода
     */
    private static void processPackets(Socket socket) throws IOException {
        DataInputStream dataIn = new DataInputStream(socket.getInputStream()); // Создаем поток для чтения данных
        int expectedPacketCount = readExpectedPacketCount(dataIn); // Читаем ожидаемое количество пакетов
        int packetSize = dataIn.readInt(); // Читаем размер пакета
        byte[] packet = new byte[packetSize + 8 + 4]; // Создаем массив для хранения пакета (размер пакета + метка времени + номер пакета)

        long startTime = System.nanoTime(); // Запоминаем время начала приема
        int receivedPacketCount = 0; // Счетчик полученных пакетов
        long totalBytes = 4; // Общее количество байт (начинаем с 4 байт для номера пакета)
        long fullLatency = 0; // Полная задержка

        // Цикл для получения пакетов
        while (receivedPacketCount < expectedPacketCount) {
            try {
                int bytesRead = readPacket(socket, packet); // Читаем пакет
                if (bytesRead == -1) break; // Если чтение завершено, выходим из цикла
                totalBytes += bytesRead; // Увеличиваем общее количество байт
                fullLatency += processPacket(packet); // Обрабатываем пакет и добавляем задержку
                receivedPacketCount++; // Увеличиваем счетчик полученных пакетов
            } catch (SocketException e) {
                System.err.println("Клиент отключен: " + e.getMessage()); // Обрабатываем отключение клиента
                break;
            } catch (IOException e) {
                System.err.println("Ошибка при чтении пакета: " + e.getMessage()); // Обрабатываем ошибки чтения
                System.err.println("Сервер может быть отключен. Завершение работы клиента.");
                break;
            }
        }
        displayStatistics(totalBytes, fullLatency, startTime, receivedPacketCount, expectedPacketCount); // Отображаем статистику
    }

    /**
     * Читает ожидаемое количество пакетов от сервера.
     *
     * @param dataIn поток для чтения * @return ожидаемое количество пакетов
     * @throws IOException если произошла ошибка ввода-вывода
     */
    private static int readExpectedPacketCount(DataInputStream dataIn) throws IOException {
        return dataIn.readInt(); // Читаем и возвращаем ожидаемое количество пакетов
    }

    /**
     * Читает пакет данных из сокета.
     *
     * @param socket сокет для получения данных
     * @param packet массив байтов для хранения пакета
     * @return количество прочитанных байт
     * @throws IOException если произошла ошибка ввода-вывода
     */
    private static int readPacket(Socket socket, byte[] packet) throws IOException {
        return socket.getInputStream().read(packet); // Читаем данные в массив байтов
    }

    /**
     * Обрабатывает полученный пакет и вычисляет задержку.
     *
     * @param packet массив байтов пакета
     * @return задержка в наносекундах
     * @throws IOException если произошла ошибка ввода-вывода
     */
    private static long processPacket(byte[] packet) throws IOException {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(packet);
             DataInputStream dataStream = new DataInputStream(byteStream)) {
            int packetNumber = dataStream.readInt(); // Читаем номер пакета
            long timestamp = dataStream.readLong(); // Читаем метку времени
            long latency = System.nanoTime() - timestamp; // Вычисляем задержку
            System.out.println("Получен " + packetNumber + " пакет (задержка: " + latency + " нс)..."); // Выводим информацию о полученном пакете
            return latency; // Возвращаем задержку
        }
    }

    /**
     * Отображает статистику о полученных пакетах.
     *
     * @param totalBytes          общее количество полученных байт
     * @param fullLatency         полная задержка
     * @param startTime           время начала приема
     * @param receivedPacketCount количество полученных пакетов
     * @param expectedPacketCount ожидаемое количество пакетов
     */
    private static void displayStatistics(long totalBytes, long fullLatency, long startTime, int receivedPacketCount, int expectedPacketCount) {
        long endTime = System.nanoTime(); // Запоминаем время окончания приема
        long duration = endTime - startTime; // Вычисляем общее время приема

        // Выводим статистику
        System.out.println();
        System.out.println("Получено " + totalBytes + " байт.");
        System.out.println("Время задержки: " + fullLatency / 1000000.0 + " мс");
        System.out.println("Общее время приема: " + duration / 1000000.0 + " мс");
        System.out.println("Скорость приема: " + (totalBytes / (duration / 1000000000.0)) + " байт/с");

        int lostPacketCount = expectedPacketCount - receivedPacketCount; // Вычисляем количество потерянных пакетов
        System.out.println("Ожидаемое количество пакетов: " + expectedPacketCount);
        System.out.println("Получено пакетов: " + receivedPacketCount);
        System.out.println("Потеряно пакетов: " + lostPacketCount);
    }
}