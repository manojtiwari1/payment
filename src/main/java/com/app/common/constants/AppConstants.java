package com.app.common.constants;

import java.util.Set;

/**
 * @author Manoj Tiwari
 * @since 01-04-2026
 * @implSpec This class contains all the constants used across the application.
 * It includes character sets, allowed MIME types for file uploads, and common HTTP header names.
 * This centralization of constants promotes consistency and maintainability throughout the codebase.
 */

public class AppConstants {


    public static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";

    public static final String UPPERCASE = LOWERCASE.toUpperCase();

    public static final String DIGITS = "0123456789";

    public static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    //UTF-8
    public static final String CHARSET = "UTF-8";

    public static final String CONTENT_DISPOSITION = "Content-Disposition";

    public static final String CONTENT_ENCODING = "Content-Encoding";

    public static final String CONTENT_LANGUAGE = "Content-Language";

    public static final String CONTENT_TYPE = "Content-Type";

    public static final String CONTENT_MD5 = "Content-MD5";

    public static final String CONTENT_RANGE = "Content-Range";

    public static final String CONTENT_LENGTH = "Content-Length";

    public static final String CONTENT_LOCATION = "Content-Location";

    public static final String SES_VERIFIED_EMAIL = "no-reply@worked.com";

    public static final String AUTH_HEADER = "Authorization";

    public static final String BEARER_PREFIX = "Bearer ";


}
