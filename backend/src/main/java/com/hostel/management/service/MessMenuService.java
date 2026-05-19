package com.hostel.management.service;

import com.hostel.management.dto.MessMenuRequest;
import com.hostel.management.model.MessMenu;
import com.hostel.management.repository.MessMenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessMenuService {
    private final MessMenuRepository repository;

    public MessMenuService(MessMenuRepository repository) { this.repository = repository; }

    public List<MessMenu> getMenu(LocalDate from, LocalDate to) {
        if (from == null) from = LocalDate.now();
        if (to == null) to = from.plusDays(6);
        return repository.findByMenuDateBetweenOrderByMenuDateAscMealTypeAsc(from, to);
    }

    @Transactional
    public MessMenu saveMenu(MessMenuRequest request) {
        MessMenu menu = repository.findByMenuDateAndMealType(request.getMenuDate(), request.getMealType()).orElse(new MessMenu());
        menu.setMenuDate(request.getMenuDate());
        menu.setMealType(request.getMealType());
        menu.setSeason(request.getSeason() != null ? request.getSeason() : seasonFor(request.getMenuDate()));
        menu.setItems(request.getItems());
        menu.setVegetables(request.getVegetables());
        menu.setMealTime(defaultMealTime(request.getMealType(), request.getMealTime()));
        menu.setNotes(request.getNotes());
        return repository.save(menu);
    }

    @Transactional public void delete(Long id) { repository.deleteById(id); }

    public byte[] exportMenuPdf(LocalDate from, LocalDate to) {
        List<MessMenu> rows = new ArrayList<>(getMenu(from, to));
        rows.sort((a, b) -> {
            int dateCompare = a.getMenuDate().compareTo(b.getMenuDate());
            if (dateCompare != 0) return dateCompare;
            return Integer.compare(mealOrder(a.getMealType()), mealOrder(b.getMealType()));
        });
        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end = to != null ? to : start.plusDays(6);

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfTableState state = new PdfTableState(document, start, end);
            state.newPage();

            if (rows.isEmpty()) {
                state.ensureSpace(40);
                drawText(state.content, "No mess menu saved for this date range.", state.margin, state.y, PDType1Font.HELVETICA, 11);
            }

            for (MessMenu row : rows) {
                String[] values = {
                        formatDate(row.getMenuDate()),
                        row.getMealType() == null ? "" : row.getMealType().name(),
                        nullToBlank(row.getMealTime()),
                        row.getSeason() == null ? "" : row.getSeason().name(),
                        nullToBlank(row.getItems()),
                        nullToBlank(row.getVegetables()),
                        nullToBlank(row.getNotes())
                };

                List<List<String>> wrapped = new ArrayList<>();
                int maxLines = 1;
                for (int i = 0; i < values.length; i++) {
                    List<String> lines = wrapText(cleanPdfText(values[i]), state.colWidths[i] - 10, PDType1Font.HELVETICA, 9);
                    wrapped.add(lines);
                    maxLines = Math.max(maxLines, lines.size());
                }

                float rowHeight = Math.max(34, 16 + (maxLines * 12));
                state.ensureSpace(rowHeight + 10);
                drawRowBox(state, rowHeight);

                float x = state.margin;
                for (int i = 0; i < wrapped.size(); i++) {
                    PDType1Font font = (i == 1) ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
                    float textY = state.y - 16;
                    for (String line : wrapped.get(i)) {
                        drawText(state.content, line, x + 5, textY, font, 9);
                        textY -= 12;
                    }
                    x += state.colWidths[i];
                }
                state.y -= rowHeight;
            }

            if (state.content != null) state.content.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Could not export mess menu PDF", e);
        }
    }

    private static class PdfTableState {
        final PDDocument document;
        final LocalDate start;
        final LocalDate end;
        final float margin = 30;
        final float[] colWidths = {70, 75, 100, 65, 185, 165, 120};
        PDPageContentStream content;
        float y;

        PdfTableState(PDDocument document, LocalDate start, LocalDate end) {
            this.document = document;
            this.start = start;
            this.end = end;
        }

        void newPage() throws IOException {
            if (content != null) content.close();
            PDPage page = new PDPage(PDRectangle.A4);
            page.setMediaBox(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - 32;
            drawHeader();
        }

        void ensureSpace(float requiredHeight) throws IOException {
            if (y - requiredHeight < 40) newPage();
        }

        void drawHeader() throws IOException {
            drawText(content, "HOSTEL MESS MENU", margin, y, PDType1Font.HELVETICA_BOLD, 18);
            drawText(content, "Date Range: " + start + " to " + end, margin, y - 20, PDType1Font.HELVETICA, 10);
            y -= 44;

            content.setLineWidth(0.8f);
            content.moveTo(margin, y + 8);
            content.lineTo(margin + totalWidth(colWidths), y + 8);
            content.stroke();

            String[] headers = {"DATE", "MEAL", "TIME", "SEASON", "MENU ITEMS", "VEGETABLES", "NOTES"};
            float x = margin;
            for (int i = 0; i < headers.length; i++) {
                drawText(content, headers[i], x + 5, y - 8, PDType1Font.HELVETICA_BOLD, 9);
                x += colWidths[i];
            }
            y -= 24;

            content.moveTo(margin, y + 8);
            content.lineTo(margin + totalWidth(colWidths), y + 8);
            content.stroke();
        }
    }

    private static void drawRowBox(PdfTableState state, float rowHeight) throws IOException {
        float x = state.margin;
        float top = state.y + 4;
        float bottom = state.y - rowHeight + 4;
        state.content.setLineWidth(0.25f);
        state.content.moveTo(state.margin, top);
        state.content.lineTo(state.margin + totalWidth(state.colWidths), top);
        state.content.stroke();
        for (float w : state.colWidths) {
            state.content.moveTo(x, top);
            state.content.lineTo(x, bottom);
            state.content.stroke();
            x += w;
        }
        state.content.moveTo(x, top);
        state.content.lineTo(x, bottom);
        state.content.stroke();
        state.content.moveTo(state.margin, bottom);
        state.content.lineTo(state.margin + totalWidth(state.colWidths), bottom);
        state.content.stroke();
    }

    private static float totalWidth(float[] widths) {
        float total = 0;
        for (float width : widths) total += width;
        return total;
    }

    private static void drawText(PDPageContentStream content, String text, float x, float y, PDType1Font font, float fontSize) throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text == null ? "" : text);
        content.endText();
    }

    private static List<String> wrapText(String text, float maxWidth, PDType1Font font, float fontSize) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }
        String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.getStringWidth(candidate) / 1000 * fontSize <= maxWidth) {
                line = new StringBuilder(candidate);
            } else {
                if (line.length() > 0) lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    private static int mealOrder(MessMenu.MealType mealType) {
        if (mealType == null) return 99;
        return switch (mealType) {
            case BREAKFAST -> 1;
            case LUNCH -> 2;
            case SNACKS -> 3;
            case DINNER -> 4;
        };
    }

    private String vegetablesFor(MessMenu.Season season) {
        return switch (season) {
            case SUMMER -> "Lauki, tori, bhindi, cucumber salad, seasonal light sabzi";
            case WINTER -> "Gobi, matar, palak, carrot, methi, seasonal green sabzi";
            case MONSOON -> "Aloo jeera, dry mixed veg, bhindi, beans, light seasonal sabzi";
            case SPRING -> "Mixed veg, beans, peas, carrot, cabbage";
            case AUTUMN -> "Aloo gobhi, cabbage, pumpkin, beans, mixed veg";
        };
    }

    private String defaultMealTime(MessMenu.MealType mealType, String value) {
        if (value != null && !value.isBlank()) return value.trim();
        if (mealType == null) return "";
        return switch (mealType) {
            case BREAKFAST -> "07:30 AM - 09:00 AM";
            case LUNCH -> "12:30 PM - 02:00 PM";
            case SNACKS -> "05:00 PM - 06:00 PM";
            case DINNER -> "08:00 PM - 10:00 PM";
        };
    }

    private String cleanPdfText(String value) { return value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", " "); }
    private String nullToBlank(String value) { return value == null ? "" : value; }

    public MessMenu.Season seasonFor(LocalDate date) {
        Month m = date.getMonth();
        return switch (m) {
            case APRIL, MAY, JUNE -> MessMenu.Season.SUMMER;
            case JULY, AUGUST, SEPTEMBER -> MessMenu.Season.MONSOON;
            case NOVEMBER, DECEMBER, JANUARY, FEBRUARY -> MessMenu.Season.WINTER;
            case MARCH -> MessMenu.Season.SPRING;
            default -> MessMenu.Season.AUTUMN;
        };
    }

    public Map<String, String> seasonalSuggestion(LocalDate date) {
        MessMenu.Season s = seasonFor(date != null ? date : LocalDate.now());
        Map<String, String> map = new LinkedHashMap<>();
        if (s == MessMenu.Season.SUMMER) {
            map.put("BREAKFAST", "Poha/Upma, curd, banana, milk/tea");
            map.put("LUNCH", "Roti, rice, dal, seasonal sabzi, curd, salad");
            map.put("SNACKS", "Lemon water/buttermilk, light snacks");
            map.put("DINNER", "Roti, rice, dal, light sabzi, salad");
        } else if (s == MessMenu.Season.WINTER) {
            map.put("BREAKFAST", "Paratha, curd/pickle, tea/milk");
            map.put("LUNCH", "Roti, rice, dal, seasonal green sabzi, salad");
            map.put("SNACKS", "Tea, roasted chana/poha");
            map.put("DINNER", "Roti, rice, dal, paneer/soyabean/veg curry");
        } else if (s == MessMenu.Season.MONSOON) {
            map.put("BREAKFAST", "Idli/poha, tea, fruit");
            map.put("LUNCH", "Roti, rice, dal, dry sabzi, salad");
            map.put("SNACKS", "Tea, light pakora/namkeen");
            map.put("DINNER", "Khichdi/roti, dal, sabzi, pickle");
        } else {
            map.put("BREAKFAST", "Poha/upma/paratha, tea/milk");
            map.put("LUNCH", "Roti, rice, dal, sabzi, salad");
            map.put("SNACKS", "Tea and snacks");
            map.put("DINNER", "Roti, rice, dal, sabzi");
        }
        map.put("VEGETABLES", vegetablesFor(s));
        map.put("season", s.name());
        return map;
    }
}
