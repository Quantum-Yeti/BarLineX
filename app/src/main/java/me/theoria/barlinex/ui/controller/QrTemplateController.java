package me.theoria.barlinex.ui.controller;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;

import me.theoria.barlinex.R;
import me.theoria.barlinex.domain.barcode.QrContentBuilder;

public class QrTemplateController {

    private static final String TYPE_TEXT = "Text / URL";
    private static final String TYPE_WIFI = "Wi-Fi";
    private static final String TYPE_CONTACT = "Contact";
    private static final String TYPE_EMAIL = "Email";
    private static final String TYPE_SMS = "SMS";
    private static final String TYPE_EVENT = "Event";
    private static final String TYPE_LOCATION = "Location";

    private final MaterialAutoCompleteTextView templateDropdown;
    private final View contentLabel;
    private final TextInputLayout contentInputLayout;
    private final BarcodeInputController inputController;

    private final View wifiFields;
    private final View contactFields;
    private final View emailFields;
    private final View smsFields;
    private final View eventFields;
    private final View locationFields;

    private final EditText wifiSsid;
    private final EditText wifiPassword;
    private final MaterialAutoCompleteTextView wifiSecurity;
    private final MaterialCheckBox wifiHidden;

    private final EditText contactFirstName;
    private final EditText contactLastName;
    private final EditText contactPhone;
    private final EditText contactEmail;
    private final EditText contactOrganization;

    private final EditText emailAddress;
    private final EditText emailSubject;
    private final EditText emailBody;

    private final EditText smsPhone;
    private final EditText smsMessage;

    private final EditText eventTitle;
    private final EditText eventLocation;
    private final EditText eventStart;
    private final EditText eventEnd;
    private final EditText eventDescription;

    private final EditText locationLatitude;
    private final EditText locationLongitude;

    private String selectedTemplate = TYPE_TEXT;
    private String savedTextContent = "";

    public QrTemplateController(
            View qrContentTypeSection,
            MaterialAutoCompleteTextView templateDropdown,
            View contentLabel,
            TextInputLayout contentInputLayout,
            BarcodeInputController inputController
    ) {
        this.templateDropdown = templateDropdown;
        this.contentLabel = contentLabel;
        this.contentInputLayout = contentInputLayout;
        this.inputController = inputController;

        wifiFields = qrContentTypeSection.findViewById(R.id.qrWifiFields);
        contactFields = qrContentTypeSection.findViewById(R.id.qrContactFields);
        emailFields = qrContentTypeSection.findViewById(R.id.qrEmailFields);
        smsFields = qrContentTypeSection.findViewById(R.id.qrSmsFields);
        eventFields = qrContentTypeSection.findViewById(R.id.qrEventFields);
        locationFields = qrContentTypeSection.findViewById(R.id.qrLocationFields);

        wifiSsid = findEditText(qrContentTypeSection, R.id.qrWifiSsid);
        wifiPassword = findEditText(qrContentTypeSection, R.id.qrWifiPassword);
        wifiSecurity = qrContentTypeSection.findViewById(R.id.qrWifiSecurity);
        wifiHidden = qrContentTypeSection.findViewById(R.id.qrWifiHidden);

        contactFirstName = findEditText(qrContentTypeSection, R.id.qrContactFirstName);
        contactLastName = findEditText(qrContentTypeSection, R.id.qrContactLastName);
        contactPhone = findEditText(qrContentTypeSection, R.id.qrContactPhone);
        contactEmail = findEditText(qrContentTypeSection, R.id.qrContactEmail);
        contactOrganization = findEditText(qrContentTypeSection, R.id.qrContactOrganization);

        emailAddress = findEditText(qrContentTypeSection, R.id.qrEmailAddress);
        emailSubject = findEditText(qrContentTypeSection, R.id.qrEmailSubject);
        emailBody = findEditText(qrContentTypeSection, R.id.qrEmailBody);

        smsPhone = findEditText(qrContentTypeSection, R.id.qrSmsPhone);
        smsMessage = findEditText(qrContentTypeSection, R.id.qrSmsMessage);

        eventTitle = findEditText(qrContentTypeSection, R.id.qrEventTitle);
        eventLocation = findEditText(qrContentTypeSection, R.id.qrEventLocation);
        eventStart = findEditText(qrContentTypeSection, R.id.qrEventStart);
        eventEnd = findEditText(qrContentTypeSection, R.id.qrEventEnd);
        eventDescription = findEditText(qrContentTypeSection, R.id.qrEventDescription);

        locationLatitude = findEditText(qrContentTypeSection, R.id.qrLocationLatitude);
        locationLongitude = findEditText(qrContentTypeSection, R.id.qrLocationLongitude);

        setupTemplateDropdown();
        setupWifiSecurityDropdown();
        setupFieldWatchers();
        showTemplateFields(TYPE_TEXT);
    }

    private EditText findEditText(View root, int viewId) {
        return root.findViewById(viewId);
    }

    private void setupTemplateDropdown() {
        List<String> templates = Arrays.asList(
                TYPE_TEXT,
                TYPE_WIFI,
                TYPE_CONTACT,
                TYPE_EMAIL,
                TYPE_SMS,
                TYPE_EVENT,
                TYPE_LOCATION
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                templateDropdown.getContext(),
                android.R.layout.simple_dropdown_item_1line,
                templates
        ) {
            @Override
            public View getView(int position, View convertView,
                                android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                android.widget.TextView textView =
                        (android.widget.TextView) view;

                textView.setTextColor(
                        android.graphics.Color.WHITE
                );

                return view;
            }
        };

        templateDropdown.setAdapter(adapter);
        templateDropdown.setText(TYPE_TEXT, false);

        templateDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String template = adapter.getItem(position);

            if (template != null) {
                selectTemplate(template);
            }
        });
    }

    private void setupWifiSecurityDropdown() {
        String[] securityOptions = {
                "WPA/WPA2",
                "WEP",
                "None"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                templateDropdown.getContext(),
                android.R.layout.simple_dropdown_item_1line,
                securityOptions
        );

        wifiSecurity.setAdapter(adapter);
        wifiSecurity.setText(securityOptions[0], false);

        wifiSecurity.setOnItemClickListener((parent, view, position, id) ->
                updatePayload()
        );
    }

    private void setupFieldWatchers() {
        addWatcher(wifiSsid);
        addWatcher(wifiPassword);

        addWatcher(contactFirstName);
        addWatcher(contactLastName);
        addWatcher(contactPhone);
        addWatcher(contactEmail);
        addWatcher(contactOrganization);

        addWatcher(emailAddress);
        addWatcher(emailSubject);
        addWatcher(emailBody);

        addWatcher(smsPhone);
        addWatcher(smsMessage);

        addWatcher(eventTitle);
        addWatcher(eventLocation);
        addWatcher(eventStart);
        addWatcher(eventEnd);
        addWatcher(eventDescription);

        addWatcher(locationLatitude);
        addWatcher(locationLongitude);

        wifiHidden.setOnCheckedChangeListener(
                (buttonView, isChecked) -> updatePayload()
        );
    }

    private void addWatcher(EditText editText) {
        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
                updatePayload();
            }

            @Override
            public void afterTextChanged(android.text.Editable editable) {
            }
        });
    }

    public void selectTemplate(String template) {
        if (template == null) {
            return;
        }

        if (!isKnownTemplate(template)) {
            return;
        }

        if (TYPE_TEXT.equals(selectedTemplate)) {
            savedTextContent =
                    inputController.getBarcodeContentInput().getText().toString();
        }

        selectedTemplate = template;

        templateDropdown.setText(template, false);
        showTemplateFields(template);

        if (TYPE_TEXT.equals(template)) {
            inputController.setBarcodeContent(savedTextContent);
            inputController.refreshPreviewNow();
        } else {
            updatePayload();
        }
    }

    private boolean isKnownTemplate(String template) {
        return TYPE_TEXT.equals(template)
                || TYPE_WIFI.equals(template)
                || TYPE_CONTACT.equals(template)
                || TYPE_EMAIL.equals(template)
                || TYPE_SMS.equals(template)
                || TYPE_EVENT.equals(template)
                || TYPE_LOCATION.equals(template);
    }

    private void showTemplateFields(String template) {
        boolean isText = TYPE_TEXT.equals(template);

        contentLabel.setVisibility(isText ? View.VISIBLE : View.GONE);
        contentInputLayout.setVisibility(isText ? View.VISIBLE : View.GONE);

        wifiFields.setVisibility(
                TYPE_WIFI.equals(template) ? View.VISIBLE : View.GONE
        );

        contactFields.setVisibility(
                TYPE_CONTACT.equals(template) ? View.VISIBLE : View.GONE
        );

        emailFields.setVisibility(
                TYPE_EMAIL.equals(template) ? View.VISIBLE : View.GONE
        );

        smsFields.setVisibility(
                TYPE_SMS.equals(template) ? View.VISIBLE : View.GONE
        );

        eventFields.setVisibility(
                TYPE_EVENT.equals(template) ? View.VISIBLE : View.GONE
        );

        locationFields.setVisibility(
                TYPE_LOCATION.equals(template) ? View.VISIBLE : View.GONE
        );
    }

    private void updatePayload() {
        String payload;

        switch (selectedTemplate) {
            case TYPE_WIFI:
                payload = QrContentBuilder.buildWifi(
                        getText(wifiSsid),
                        getText(wifiPassword),
                        wifiSecurity.getText().toString(),
                        wifiHidden.isChecked()
                );
                break;

            case TYPE_CONTACT:
                payload = QrContentBuilder.buildContact(
                        getText(contactFirstName),
                        getText(contactLastName),
                        getText(contactPhone),
                        getText(contactEmail),
                        getText(contactOrganization)
                );
                break;

            case TYPE_EMAIL:
                payload = QrContentBuilder.buildEmail(
                        getText(emailAddress),
                        getText(emailSubject),
                        getText(emailBody)
                );
                break;

            case TYPE_SMS:
                payload = QrContentBuilder.buildSms(
                        getText(smsPhone),
                        getText(smsMessage)
                );
                break;

            case TYPE_EVENT:
                payload = QrContentBuilder.buildEvent(
                        getText(eventTitle),
                        getText(eventLocation),
                        getText(eventStart),
                        getText(eventEnd),
                        getText(eventDescription)
                );
                break;

            case TYPE_LOCATION:
                payload = QrContentBuilder.buildLocation(
                        getText(locationLatitude),
                        getText(locationLongitude)
                );
                break;

            case TYPE_TEXT:
            default:
                return;
        }

        inputController.setBarcodeContent(payload);
        inputController.refreshPreviewNow();
    }

    private String getText(EditText editText) {
        return editText.getText() == null
                ? ""
                : editText.getText().toString().trim();
    }

    public String getSelectedTemplate() {
        return selectedTemplate;
    }
}
