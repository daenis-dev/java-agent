package com.greenpalmsolutions.agents.knowledge.internal;

import com.greenpalmsolutions.agents.knowledge.api.ReadKnowledge;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class FileKnowledgeService implements ReadKnowledge {

    private final Path root;

    FileKnowledgeService(@Value("${agent.knowledge-path}") String path) {
        root = Path.of(path).toAbsolutePath().normalize();
    }

    @Override
    public String status() {
        return Files.isDirectory(root)
                ? "Connected: " + root
                : "Not configured: clone your knowledge repository to " + root;
    }

    @Override
    public String relevantContext(String question) {
        if (!Files.isDirectory(root)) {
            return "Knowledge repository not configured yet.";
        }

        List<Path> files;

        try (Stream<Path> walk = Files.walk(root, 6)) {
            files = walk.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".md"))
                    .filter(file -> !file.toString().contains("/.git/"))
                    .sorted(Comparator.comparing(Path::toString))
                    .limit(100)
                    .toList();
        } catch (IOException exception) {
            return "Knowledge unavailable: " + exception.getMessage();
        }

        Set<String> terms = Arrays
                .stream(question
                        .toLowerCase(Locale.ROOT)
                        .split("[^a-z0-9]+"))
                .filter(term -> term.length() > 3)
                .collect(Collectors.toSet());

        record Match(Path path, String body, int score) {
        }

        List<Match> matches = new ArrayList<>();

        for (Path file : files) {
            try {
                String body = Files.readString(file);

                if (body.length() > 120_000) {
                    continue;
                }

                String searchable = (file + " " + body).toLowerCase(Locale.ROOT);
                int score = (int) terms
                        .stream()
                        .filter(searchable::contains)
                        .count();

                if (score > 0 || terms.isEmpty()) {
                    matches.add(new Match(file, body, score));
                }
            } catch (IOException exception) {
            }
        }

        matches.sort(Comparator.comparingInt(Match::score).reversed());
        StringBuilder context = new StringBuilder();

        for (Match match : matches
                .stream()
                .limit(8)
                .toList()) {
            String excerpt = match.body().substring(0, Math.min(3500, match.body().length()));

            if (context.length() + excerpt.length() > 14_000) {
                break;
            }

            context.append("\nSOURCE: ")
                    .append(root.relativize(match.path()))
                    .append("\n")
                    .append(excerpt)
                    .append("\n");
        }

        return context.isEmpty()
                ? "No matching knowledge documents found."
                : context.toString();
    }

    @Override
    public synchronized String writeAgentInterpretation(String title, String content) {
        if (title == null || !title.matches("[a-zA-Z0-9][a-zA-Z0-9 -]{2,70}")) {
            throw new IllegalArgumentException("Title must be 3–71 alphanumeric/space/hyphen characters");
        }

        if (content == null || content.isBlank() || content.length() > 20_000) {
            throw new IllegalArgumentException("Invalid interpretation content");
        }

        Path directory = root.resolve("interpretations/agent").normalize();

        if (!directory.startsWith(root)) {
            throw new IllegalArgumentException("Invalid knowledge path");
        }

        String slug = title.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        Path target = directory.resolve(slug + ".md");

        try {
            Files.createDirectories(directory);
            Files.writeString(target, "# " + title + "\n\n" + content + "\n", StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException exception) {
            throw new IllegalArgumentException("Interpretation already exists; review it in Git before revising");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write interpretation", exception);
        }

        return root.relativize(target).toString();
    }
}
