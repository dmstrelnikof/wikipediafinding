import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.awt.Desktop;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;

public class oop1 {
    private static final String WIKI_API = "https://ru.wikipedia.org/w/api.php";
    private static final String WIKI_URL = "https://ru.wikipedia.org/wiki/";

    static class SearchResult {
        String title;
        String url;

        SearchResult(String title) {
            this.title = title;
            this.url = "";
        }
    }

    public static void main(String[] args) {
        try {
            // Отключаем предупреждения IMK для macOS
            System.setProperty("apple.awt.UIElement", "true");
            System.setProperty("apple.awt.headless", "true");

            // Устанавливаем кодировку UTF-8 для консоли
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("console.encoding", "UTF-8");

            // Создаем Scanner с явным указанием UTF-8
            Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
            System.out.println("Введите поисковый запрос:");
            String searchQuery = scanner.nextLine();

            ArrayList<SearchResult> results = search(searchQuery);

            if (results.isEmpty()) {
                System.out.println("Ничего не найдено");
                scanner.close();
                return;
            }

            for (int i = 0; i < results.size(); i++) {
                System.out.println((i + 1) + ". " + results.get(i).title);
            }

            System.out.println("\nВыберите номер статьи (или 0 для выхода):");
            int choice = scanner.nextInt();

            if (choice > 0 && choice <= results.size()) {
                openInBrowser(results.get(choice - 1).title);
            }

            scanner.close();

        } catch (Exception e) {
            System.out.println("Произошла ошибка: " + e.getMessage());
        }
    }

    private static ArrayList<SearchResult> search(String query) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String urlString = WIKI_API + "?action=opensearch&format=json&search=" + encodedQuery + "&limit=10";

        URL apiUrl = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        // Получаем URL'ы статей
        int urlsStart = response.lastIndexOf("[");
        int urlsEnd = response.lastIndexOf("]");
        String urlsJson = response.substring(urlsStart + 1, urlsEnd);

        ArrayList<SearchResult> results = parseResults(response.toString());

        // Добавляем URL'ы к результатам
        String[] urls = urlsJson.split("\",\"");
        for (int i = 0; i < Math.min(results.size(), urls.length); i++) {
            results.get(i).url = urls[i].replace("\"", "").trim();
        }

        return results;
    }

    private static ArrayList<SearchResult> parseResults(String response) {
        ArrayList<SearchResult> results = new ArrayList<>();

        // Ищем начало массива с заголовками
        int titlesStart = response.indexOf("[", response.indexOf("[") + 1);
        if (titlesStart == -1) return results;

        int titlesEnd = findMatchingBracket(response, titlesStart);
        if (titlesEnd == -1) return results;

        String titlesJson = response.substring(titlesStart + 1, titlesEnd);

        // Парсим заголовки и декодируем Unicode
        String[] titles = titlesJson.split("\",\"");
        for (String title : titles) {
            title = title.replace("\"", "").trim();
            if (!title.isEmpty()) {
                // Декодируем Unicode escape-последовательности
                String decodedTitle = decodeUnicode(title);
                results.add(new SearchResult(decodedTitle));
            }
        }

        return results;
    }

    private static String decodeUnicode(String input) {
        StringBuilder str = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == '\\' && i + 1 < input.length() && input.charAt(i + 1) == 'u') {
                // Читаем 4 шестнадцатеричные цифры
                String hex = input.substring(i + 2, i + 6);
                str.append((char) Integer.parseInt(hex, 16));
                i += 5;
            } else {
                str.append(ch);
            }
        }
        return str.toString();
    }

    private static int findMatchingBracket(String text, int openBracketPos) {
        int counter = 1;
        for (int i = openBracketPos + 1; i < text.length(); i++) {
            if (text.charAt(i) == '[') counter++;
            if (text.charAt(i) == ']') counter--;
            if (counter == 0) return i;
        }
        return -1;
    }

    private static void openInBrowser(String title) {
        try {
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8)
                    .replace("+", "_");  // Заменяем + на _ для Wikipedia
            Desktop.getDesktop().browse(new URI(WIKI_URL + encodedTitle));
        } catch (Exception e) {
            System.out.println("Не удалось открыть браузер: " + e.getMessage());
        }
    }
}
