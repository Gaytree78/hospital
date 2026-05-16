package com.example.hostipal_info;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class AddPatientActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://tsm.ecssofttech.com/Gaytree/hospital/";
    private static final String TAG = "AddPatient";

    private enum UploadTarget { GENERAL, BLOOD, URINE }

    private UploadTarget currentTarget = UploadTarget.GENERAL;

    EditText etFullName, etAge, etMobile, etAddress,
            etEmergency, etSymptoms, etDiagnosis,
            etAbhaNumber, etTreatment;

    Spinner spinnerGender, spinnerBlood;

    Button btnBrowseFiles,
            btnUploadBloodReport,
            btnUploadUrineReport;

    ImageView imgBloodReportPreview,
            imgUrineReportPreview;

    TextView tvBloodReportName,
            tvUrineReportName;

    Uri generalFileUri = null;
    Uri bloodReportUri = null;
    Uri urineReportUri = null;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) handlePickedFile(uri);
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_patient);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null)
            toolbar.setNavigationOnClickListener(v -> finish());

        etFullName   = findViewById(R.id.etFullName);
        etAge        = findViewById(R.id.etAge);
        etMobile     = findViewById(R.id.etMobileNumber);
        etAddress    = findViewById(R.id.etHomeAddress);
        etEmergency  = findViewById(R.id.etEmergencyContact);
        etSymptoms   = findViewById(R.id.etSymptoms);
        etDiagnosis  = findViewById(R.id.etPrimaryDiagnosis);
        etAbhaNumber = findViewById(R.id.etAbhaNumber);

        etTreatment  = findViewById(R.id.etTreatment);

        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerBlood  = findViewById(R.id.spinnerBloodGroup);


        btnUploadBloodReport = findViewById(R.id.btnUploadBloodReport);
        btnUploadUrineReport = findViewById(R.id.btnUploadUrineReport);

        imgBloodReportPreview = findViewById(R.id.imgBloodReportPreview);
        imgUrineReportPreview = findViewById(R.id.imgUrineReportPreview);

        tvBloodReportName = findViewById(R.id.tvBloodReportName);
        tvUrineReportName = findViewById(R.id.tvUrineReportName);

        // ── Update button labels to mention PDF support ────────────
        if (btnUploadBloodReport != null)
            btnUploadBloodReport.setText("Upload Blood Report (image / PDF)");
        if (btnUploadUrineReport != null)
            btnUploadUrineReport.setText("Upload Urine Report (image / PDF)");
        if (btnBrowseFiles != null)
            btnBrowseFiles.setText("Browse Files (image / PDF)");

        // ── Click listeners ────────────────────────────────────────


        if (btnBrowseFiles != null)
            btnBrowseFiles.setOnClickListener(v -> {
                currentTarget = UploadTarget.GENERAL;
                openFilePicker();
            });

        if (btnUploadBloodReport != null)
            btnUploadBloodReport.setOnClickListener(v -> {
                currentTarget = UploadTarget.BLOOD;
                openFilePicker();
            });

        if (btnUploadUrineReport != null)
            btnUploadUrineReport.setOnClickListener(v -> {
                currentTarget = UploadTarget.URINE;
                openFilePicker();
            });

        View btnCreate = findViewById(R.id.btnCreateRecord);
        if (btnCreate != null) btnCreate.setOnClickListener(v -> savePatient());

        View btnCancel = findViewById(R.id.btnCancel);
        if (btnCancel != null) btnCancel.setOnClickListener(v -> finish());
    }

    // ─────────────────────────────────────────────
    //  File Picker — accepts images AND PDFs
    // ─────────────────────────────────────────────

    private void openFilePicker() {
        // Accept image gallery pick
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");

        // Accept files: images + PDF
        Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileIntent.setType("*/*");
        fileIntent.putExtra(
                Intent.EXTRA_MIME_TYPES,
                new String[]{"image/jpeg", "image/png", "application/pdf"}
        );
        fileIntent.addCategory(Intent.CATEGORY_OPENABLE);

        Intent chooser = Intent.createChooser(fileIntent, "Select Image or PDF");
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{galleryIntent});
        filePickerLauncher.launch(chooser);
    }

    private void handlePickedFile(Uri uri) {
        String fileName = getFileNameFromUri(uri);
        boolean isPdf   = fileName.toLowerCase().endsWith(".pdf");

        switch (currentTarget) {
            case GENERAL:
                generalFileUri = uri;
                Toast.makeText(this,
                        (isPdf ? "📄 PDF selected: " : "📎 File selected: ") + fileName,
                        Toast.LENGTH_SHORT).show();
                break;

            case BLOOD:
                bloodReportUri = uri;
                updateReportUI(uri, fileName, isPdf,
                        imgBloodReportPreview, tvBloodReportName,
                        btnUploadBloodReport, "Blood Report");
                break;

            case URINE:
                urineReportUri = uri;
                updateReportUI(uri, fileName, isPdf,
                        imgUrineReportPreview, tvUrineReportName,
                        btnUploadUrineReport, "Urine Report");
                break;
        }
    }

    /**
     * For images: shows thumbnail preview.
     * For PDFs: shows filename label, hides image preview.
     */
    private void updateReportUI(
            Uri uri,
            String fileName,
            boolean isPdf,
            ImageView preview,
            TextView label,
            Button btn,
            String title
    ) {
        if (isPdf) {
            // PDF — no image preview, just show filename
            if (preview != null) preview.setVisibility(View.GONE);
            if (label  != null) {
                label.setText("📄 " + fileName);
                label.setVisibility(View.VISIBLE);
            }
        } else {
            // Image — show thumbnail
            String mime = getContentResolver().getType(uri);
            if (preview != null) {
                if (mime != null && mime.startsWith("image/")) {
                    preview.setImageURI(uri);
                    preview.setVisibility(View.VISIBLE);
                } else {
                    preview.setVisibility(View.GONE);
                }
            }
            if (label != null) {
                label.setText(fileName);
                label.setVisibility(View.VISIBLE);
            }
        }

        if (btn != null) btn.setText("✓ " + title + " selected");
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    private String getFileNameFromUri(Uri uri) {
        String name = "file";
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int index = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) name = c.getString(index);
            }
        } catch (Exception e) {
            name = uri.getLastPathSegment();
        }
        return name;
    }

    private byte[] readBytes(Uri uri) throws IOException {
        InputStream is = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = is.read(data)) != -1) buffer.write(data, 0, nRead);
        is.close();
        return buffer.toByteArray();
    }

    // ─────────────────────────────────────────────
    //  Save Patient
    // ─────────────────────────────────────────────

    private void savePatient() {
        String name   = etFullName.getText().toString().trim();
        String age    = etAge.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();

        if (name.isEmpty())   { etFullName.setError("Enter Name");   return; }
        if (age.isEmpty())    { etAge.setError("Enter Age");          return; }
        if (mobile.isEmpty()) { etMobile.setError("Enter Mobile");    return; }

        String boundary = "Boundary-" + System.currentTimeMillis();
        byte[] body;

        try {
            body = buildMultipartBody(boundary);
        } catch (Exception e) {
            Toast.makeText(this, "Build Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        final byte[] finalBody = body;
        String url = BASE_URL + "add_patient.php";

        Request<String> request = new Request<String>(
                Request.Method.POST, url,
                error -> {
                    if (error.networkResponse != null) {
                        String raw = new String(error.networkResponse.data);
                        Log.e(TAG, "Server Error Body: " + raw);
                        Toast.makeText(this,
                                "Server Error " + error.networkResponse.statusCode,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "Network Error: " + error.toString());
                        Toast.makeText(this, "Network Error", Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override public String getBodyContentType() {
                return "multipart/form-data; boundary=" + boundary;
            }
            @Override public byte[] getBody() { return finalBody; }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String raw = new String(response.data);
                Log.d(TAG, "Raw Response: " + raw);
                return Response.success(raw, HttpHeaderParser.parseCacheHeaders(response));
            }

            @Override
            protected void deliverResponse(String rawResponse) {
                Log.d(TAG, "Deliver Response: " + rawResponse);

                String cleaned = rawResponse.trim();
                int start = cleaned.indexOf("{");
                if (start < 0) {
                    Toast.makeText(AddPatientActivity.this,
                            "Invalid Response", Toast.LENGTH_LONG).show();
                    return;
                }
                if (start > 0) cleaned = cleaned.substring(start);

                try {
                    JSONObject json = new JSONObject(cleaned);
                    String status = json.optString("status", "");

                    if (status.equals("success")) {
                        String pid = json.optString("patient_id_display", "");
                        Toast.makeText(AddPatientActivity.this,
                                "Patient Added: " + pid, Toast.LENGTH_LONG).show();
                        finish();
                    } else if (status.equals("duplicate")) {
                        etMobile.setError("Mobile already exists");
                        etMobile.requestFocus();
                    } else {
                        String msg = json.optString("message", "Unknown Error");
                        Toast.makeText(AddPatientActivity.this, msg, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "JSON Parse Exception: " + e.getMessage());
                    Toast.makeText(AddPatientActivity.this,
                            "JSON Parse Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    // ─────────────────────────────────────────────
    //  Multipart Body Builder
    // ─────────────────────────────────────────────

    private byte[] buildMultipartBody(String boundary) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        String LB = "\r\n";

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("name",        etFullName.getText().toString().trim());
        fields.put("age",         etAge.getText().toString().trim());
        fields.put("gender",      spinnerGender.getSelectedItem().toString());
        fields.put("blood_group", spinnerBlood.getSelectedItem().toString());
        fields.put("mobile",      etMobile.getText().toString().trim());
        fields.put("address",     etAddress.getText().toString().trim());
        fields.put("emergency",   etEmergency.getText().toString().trim());
        fields.put("symptoms",    etSymptoms.getText().toString().trim());
        fields.put("diagnosis",   etDiagnosis.getText().toString().trim());
        fields.put("abha_number", etAbhaNumber.getText().toString().trim());
        fields.put("treatment",   etTreatment.getText().toString().trim());

        for (Map.Entry<String, String> e : fields.entrySet()) {
            dos.writeBytes("--" + boundary + LB);
            dos.writeBytes("Content-Disposition: form-data; name=\"" + e.getKey() + "\"" + LB);
            dos.writeBytes(LB);
            dos.write(e.getValue().getBytes("UTF-8"));
            dos.writeBytes(LB);
        }

        appendFile(dos, boundary, "general_file", generalFileUri, LB);
        appendFile(dos, boundary, "blood_report",  bloodReportUri, LB);
        appendFile(dos, boundary, "urine_report",  urineReportUri, LB);

        dos.writeBytes("--" + boundary + "--" + LB);
        dos.flush();
        return bos.toByteArray();
    }

    private void appendFile(
            DataOutputStream dos,
            String boundary,
            String fieldName,
            Uri uri,
            String LB
    ) throws IOException {
        if (uri == null) return;
        String fileName = getFileNameFromUri(uri);
        String mime     = getContentResolver().getType(uri);
        if (mime == null) mime = "application/octet-stream";
        byte[] bytes = readBytes(uri);
        dos.writeBytes("--" + boundary + LB);
        dos.writeBytes("Content-Disposition: form-data; name=\""
                + fieldName + "\"; filename=\"" + fileName + "\"" + LB);
        dos.writeBytes("Content-Type: " + mime + LB);
        dos.writeBytes(LB);
        dos.write(bytes);
        dos.writeBytes(LB);
    }
}