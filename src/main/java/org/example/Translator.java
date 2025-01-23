package org.example;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

public class Translator {
    private final Translate translate;

    public Translator() {
        translate = TranslateOptions.getDefaultInstance().getService();
    }

    public String translateToPolish(String text) {
        Translation translation = translate.translate(text,
                Translate.TranslateOption.targetLanguage("pl"));
        return translation.getTranslatedText();
    }

    public String getEnglishPronunciation(String text) {
        return translate.translate(text, Translate.TranslateOption.targetLanguage("en"))
                .getSourceLanguage();
    }
}
