package com.example.hostipal_info;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportGenerator {

    private final Context context;
    private static final String TAG         = "ReportGenerator";
    private static final int    PAGE_WIDTH  = 595;
    private static final int    PAGE_HEIGHT = 842;
    private static final int    MARGIN      = 40;
    private static final int    LINE_HEIGHT = 24;

    // ── Clinic Details ─────────────────────────────────────────────
    private static final String CLINIC_NAME    = "Krishnai Clinic, Kale";
    private static final String DOCTOR_1       = "Dr. Sanjay Kumbhar MBBS";
    private static final String DOCTOR_1_PHONE = "9272325081";
    private static final String DOCTOR_2       = "Dr. Sujata Snajay Kumbhar MBBS.MD(Pathology)";
    private static final String DOCTOR_2_PHONE = "8275487481";
    private static final String TIMING_LINE_1  = "Monday to Saturday: 06 PM – 10 PM";
    private static final String TIMING_LINE_2  = "Sunday: 10 AM : 1 PM & 04 PM : 10 PM";

    public ReportGenerator(Context context) {
        this.context = context;
    }

    // ─────────────────────────────────────────────
    //  Main entry — generates PDF file
    // ─────────────────────────────────────────────

    public File generatePDF(
            Map<String, String> patient,
            List<Map<String, String>> visits,
            String bloodReportUrl,
            String urineReportUrl
    ) throws IOException {

        Bitmap bloodBitmap = null;
        Bitmap urineBitmap = null;

        if (bloodReportUrl != null && !bloodReportUrl.isEmpty()) {
            bloodBitmap = downloadBitmap(bloodReportUrl);
        }
        if (urineReportUrl != null && !urineReportUrl.isEmpty()) {
            urineBitmap = downloadBitmap(urineReportUrl);
        }

        PdfDocument pdf = new PdfDocument();

        // Page 1: Patient Info + Visits
        PdfDocument.PageInfo page1Info =
                new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page1 = pdf.startPage(page1Info);
        drawPage1(page1.getCanvas(), patient, visits);
        pdf.finishPage(page1);

        // Page 2: Blood Report
        PdfDocument.PageInfo page2Info =
                new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create();
        PdfDocument.Page page2 = pdf.startPage(page2Info);
        drawSingleReportPage(page2.getCanvas(), bloodBitmap, bloodReportUrl,
                "BLOOD REPORT", "Blood Report");
        pdf.finishPage(page2);

        // Page 3: Urine Report
        PdfDocument.PageInfo page3Info =
                new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 3).create();
        PdfDocument.Page page3 = pdf.startPage(page3Info);
        drawSingleReportPage(page3.getCanvas(), urineBitmap, urineReportUrl,
                "URINE REPORT", "Urine Report");
        pdf.finishPage(page3);

        // Save PDF
        String patientName = patient.getOrDefault("name", "Patient")
                .replaceAll("\\s+", "_");
        String fileName = "MedicalReport_" + patientName + "_"
                + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date())
                + ".pdf";

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir != null && !dir.exists()) dir.mkdirs();

        File file = new File(dir, fileName);
        FileOutputStream fos = new FileOutputStream(file);
        pdf.writeTo(fos);
        fos.close();
        pdf.close();

        Log.d(TAG, "PDF saved to: " + file.getAbsolutePath());
        return file;
    }

    // ─────────────────────────────────────────────
    //  Clinic Header — drawn at top of EVERY page
    //  Left:  Krishnai Clinic Kale (large)
    //  Right: Doctors + Timings (bold)
    // ─────────────────────────────────────────────

    private int drawClinicHeader(Canvas canvas) {
        // Blue bar background
        Paint headerBg = new Paint();
        headerBg.setColor(Color.parseColor("#1E40AF"));
        canvas.drawRect(0, 0, PAGE_WIDTH, 90, headerBg);

        // ── LEFT: Clinic Name ──────────────────────────────────────
        Paint clinicName = new Paint();
        clinicName.setColor(Color.WHITE);
        clinicName.setTextSize(18);
        clinicName.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(CLINIC_NAME, MARGIN, 28, clinicName);

        // Small subtitle under clinic name
        Paint subtitle = new Paint();
        subtitle.setColor(Color.parseColor("#BFDBFE"));
        subtitle.setTextSize(9);
        canvas.drawText("Medical Report", MARGIN, 42, subtitle);

        // Timing lines under clinic name (bold white)
        Paint timing = new Paint();
        timing.setColor(Color.WHITE);
        timing.setTextSize(8);
        timing.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(TIMING_LINE_1, MARGIN, 58, timing);
        canvas.drawText(TIMING_LINE_2, MARGIN, 70, timing);

        // ── RIGHT: Doctors ─────────────────────────────────────────
        // Doctor Name Paint
        Paint docPaint = new Paint();
        docPaint.setColor(Color.WHITE);
        docPaint.setTextSize(9);
        docPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        docPaint.setTextAlign(Paint.Align.RIGHT);

// Degree Paint
        Paint degreePaint = new Paint();
        degreePaint.setColor(Color.parseColor("#DBEAFE"));
        degreePaint.setTextSize(7);
        degreePaint.setTextAlign(Paint.Align.RIGHT);

// Phone Paint
        Paint phonePaint = new Paint();
        phonePaint.setColor(Color.parseColor("#BFDBFE"));
        phonePaint.setTextSize(8);
        phonePaint.setTextAlign(Paint.Align.RIGHT);

// ── Doctor 1 ─────────────────────
        canvas.drawText("Dr. Sanjay Kumbhar", PAGE_WIDTH - MARGIN, 24, docPaint);
        canvas.drawText("MBBS", PAGE_WIDTH - MARGIN, 34, degreePaint);
        canvas.drawText("Mobile No : " + DOCTOR_1_PHONE, PAGE_WIDTH - MARGIN, 44, phonePaint);

// ── Doctor 2 ─────────────────────
        canvas.drawText("Dr. Sujata Sanjay Kumbhar", PAGE_WIDTH - MARGIN, 58, docPaint);
        canvas.drawText("MBBS, MD (Pathology)", PAGE_WIDTH - MARGIN, 68, degreePaint);
        canvas.drawText("Mobile No: " + DOCTOR_2_PHONE, PAGE_WIDTH - MARGIN, 78, phonePaint);

        // Bottom accent line
        Paint accent = new Paint();
        accent.setColor(Color.parseColor("#3B82F6"));
        canvas.drawRect(0, 90, PAGE_WIDTH, 94, accent);

        return 104; // y position after header
    }

    // ─────────────────────────────────────────────
    //  Page 1 — Patient Info + Visit History
    // ─────────────────────────────────────────────

    private void drawPage1(
            Canvas canvas,
            Map<String, String> patient,
            List<Map<String, String>> visits
    ) {
        int y = drawClinicHeader(canvas);

        // Generated date line
        Paint datePaint = new Paint();
        datePaint.setColor(Color.parseColor("#6B7280"));
        datePaint.setTextSize(9);
        canvas.drawText("Generated: " + new SimpleDateFormat("dd MMM yyyy HH:mm",
                Locale.getDefault()).format(new Date()), MARGIN, y, datePaint);
        y += 14;

        // Patient Info Section
        y = drawSectionHeader(canvas, y, "PATIENT INFORMATION");
        y = drawRow(canvas, y, "Name",            patient.getOrDefault("name", "N/A"));
        y = drawRow(canvas, y, "Patient ID",      "" + formatId(patient.getOrDefault("id", "0")));
        y = drawRow(canvas, y, "Age",             patient.getOrDefault("age", "N/A") + " years");
        y = drawRow(canvas, y, "Gender",          patient.getOrDefault("gender", "N/A"));
        y = drawRow(canvas, y, "Blood Group",     patient.getOrDefault("blood_group", "N/A"));
        y = drawRow(canvas, y, "Mobile",          patient.getOrDefault("mobile", "N/A"));
        y = drawRow(canvas, y, "Address",         patient.getOrDefault("address", "N/A"));
        y = drawRow(canvas, y, "Emergency",       patient.getOrDefault("emergency_contact", "N/A"));
        y = drawRow(canvas, y, "ABHA Number",     patient.getOrDefault("abha_number", "N/A"));
        y += 8;

        // Medical Info
        y = drawSectionHeader(canvas, y, "MEDICAL INFORMATION");
        y = drawRow(canvas, y, "Symptoms",  patient.getOrDefault("symptoms", "N/A"));
        y = drawRow(canvas, y, "Diagnosis", patient.getOrDefault("diagnosis", "N/A"));
        y = drawRow(canvas, y, "Treatment", patient.getOrDefault("treatment", "N/A"));

        y += 8;

        // Visit History
        if (visits != null && !visits.isEmpty()) {
            y = drawSectionHeader(canvas, y, "VISIT HISTORY");

            for (Map<String, String> visit : visits) {
                if (y > PAGE_HEIGHT - 90) break;

                Paint cardBg = new Paint();
                cardBg.setColor(Color.parseColor("#F0F7FF"));
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 65, cardBg);

                Paint border = new Paint();
                border.setColor(Color.parseColor("#BFDBFE"));
                border.setStyle(Paint.Style.STROKE);
                border.setStrokeWidth(1);
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 65, border);

                int fee = 0;
                try {
                    fee = Integer.parseInt(visit.getOrDefault("fee", "0"));
                } catch (Exception ignored) {}

                String feeText = fee > 0 ? "  •  Rs." + fee : "";

                Paint visitDatePaint = new Paint();
                visitDatePaint.setColor(Color.parseColor("#2563EB"));
                visitDatePaint.setTextSize(10);
                visitDatePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                canvas.drawText(
                        visit.getOrDefault("visit_date", "") + feeText,
                        MARGIN + 8,
                        y + 16,
                        visitDatePaint
                );

                Paint detailPaint = new Paint();
                detailPaint.setColor(Color.parseColor("#374151"));
                detailPaint.setTextSize(8);

                canvas.drawText(
                        "Symptoms: " + truncate(visit.getOrDefault("symptoms", "N/A"), 70),
                        MARGIN + 8,
                        y + 30,
                        detailPaint
                );

                canvas.drawText(
                        "Diagnosis: " + truncate(visit.getOrDefault("diagnosis", "N/A"), 70),
                        MARGIN + 8,
                        y + 44,
                        detailPaint
                );

                String notes = visit.getOrDefault("notes", "");
                if (!notes.isEmpty()) {
                    canvas.drawText(
                            "Treatment: " + truncate(notes, 70),
                            MARGIN + 8,
                            y + 58,
                            detailPaint
                    );
                }

// Reduced spacing
                y += 72;
            }
        }

        drawFooter(canvas);
    }

    // ─────────────────────────────────────────────
    //  Single Report Page (Blood / Urine)
    // ─────────────────────────────────────────────

    private void drawSingleReportPage(
            Canvas canvas,
            Bitmap bitmap,
            String reportUrl,
            String sectionTitle,
            String reportLabel
    ) {
        int y = drawClinicHeader(canvas);

        y = drawSectionHeader(canvas, y, sectionTitle);

        int availableWidth  = PAGE_WIDTH - (MARGIN * 2);

        if (bitmap != null) {
            int originalW = bitmap.getWidth();
            int originalH = bitmap.getHeight();

            int drawW, drawH;
            if (originalW > availableWidth) {
                float scale = (float) availableWidth / originalW;
                drawW = availableWidth;
                drawH = (int) (originalH * scale);
            } else {
                drawW = originalW;
                drawH = originalH;
            }

            int remainingHeight = PAGE_HEIGHT - y - 70;
            if (drawH > remainingHeight) {
                float scale = (float) remainingHeight / drawH;
                drawW = (int) (drawW * scale);
                drawH = remainingHeight;
            }

            Bitmap drawn = Bitmap.createScaledBitmap(bitmap, drawW, drawH, false);

            Paint borderPaint = new Paint();
            borderPaint.setColor(Color.parseColor("#E5E7EB"));
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(1);
            canvas.drawRect(MARGIN, y, MARGIN + drawW, y + drawH, borderPaint);

            canvas.drawBitmap(drawn, MARGIN, y, null);

        } else {
            if (reportUrl != null && !reportUrl.isEmpty()) {
                Paint urlPaint = new Paint();
                urlPaint.setColor(Color.parseColor("#2563EB"));
                urlPaint.setTextSize(10);
                canvas.drawText("View at: " + reportUrl, MARGIN + 8, y + 20, urlPaint);

                // "Not an image" note
                Paint notePaint = new Paint();
                notePaint.setColor(Color.parseColor("#6B7280"));
                notePaint.setTextSize(9);
                canvas.drawText("(PDF or non-image file — open link to view)",
                        MARGIN + 8, y + 36, notePaint);
            } else {
                drawNotAvailable(canvas, y);
            }
        }

        drawFooter(canvas);
    }

    // ─────────────────────────────────────────────
    //  Drawing Helpers
    // ─────────────────────────────────────────────

    private int drawSectionHeader(Canvas canvas, int y, String title) {
        Paint bg = new Paint();
        bg.setColor(Color.parseColor("#EFF6FF"));
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 26, bg);

        Paint text = new Paint();
        text.setColor(Color.parseColor("#1E40AF"));
        text.setTextSize(11);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(title, MARGIN + 8, y + 18, text);

        return y + 34;
    }

    private int drawRow(Canvas canvas, int y, String label, String value) {
        Paint labelPaint = new Paint();
        labelPaint.setColor(Color.parseColor("#6B7280"));
        labelPaint.setTextSize(10);
        canvas.drawText(label + ":", MARGIN + 8, y + 14, labelPaint);

        Paint valuePaint = new Paint();
        valuePaint.setColor(Color.parseColor("#111827"));
        valuePaint.setTextSize(10);
        valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(truncate(value != null ? value : "N/A", 60), 210, y + 14, valuePaint);

        Paint line = new Paint();
        line.setColor(Color.parseColor("#F3F4F6"));
        canvas.drawLine(MARGIN, y + 18, PAGE_WIDTH - MARGIN, y + 18, line);

        return y + LINE_HEIGHT;
    }

    private int drawNotAvailable(Canvas canvas, int y) {
        Paint paint = new Paint();
        paint.setColor(Color.parseColor("#9CA3AF"));
        paint.setTextSize(10);
        canvas.drawText("Not uploaded", MARGIN + 8, y + 16, paint);
        return y + 30;
    }

    private void drawFooter(Canvas canvas) {
        // Divider line
        Paint line = new Paint();
        line.setColor(Color.parseColor("#E5E7EB"));
        canvas.drawLine(MARGIN, PAGE_HEIGHT - 42, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 42, line);

        // Clinic name on left
        Paint leftText = new Paint();
        leftText.setColor(Color.parseColor("#6B7280"));
        leftText.setTextSize(8);
        leftText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(CLINIC_NAME, MARGIN, PAGE_HEIGHT - 26, leftText);

        // Generated date on right
        Paint rightText = new Paint();
        rightText.setColor(Color.parseColor("#9CA3AF"));
        rightText.setTextSize(8);
        rightText.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(
                "Generated: " + new SimpleDateFormat("dd MMM yyyy HH:mm",
                        Locale.getDefault()).format(new Date()),
                PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 26, rightText);
    }

    // ─────────────────────────────────────────────
    //  Download Bitmap from URL
    // ─────────────────────────────────────────────

    private Bitmap downloadBitmap(String urlString) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Image download response: " + responseCode + " for " + urlString);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap == null) Log.e(TAG, "BitmapFactory returned null for: " + urlString);
                return bitmap;
            } else {
                Log.e(TAG, "HTTP error " + responseCode + " for: " + urlString);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Download failed: " + e.getMessage() + " URL: " + urlString);
            return null;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (connection  != null) connection.disconnect();
            } catch (IOException ignored) {}
        }
    }

    // ─────────────────────────────────────────────
    //  Utility
    // ─────────────────────────────────────────────

    private String truncate(String text, int maxLength) {
        if (text == null) return "N/A";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private String formatId(String id) {
        try { return String.format("%05d", Integer.parseInt(id)); }
        catch (Exception e) { return id; }
    }
}