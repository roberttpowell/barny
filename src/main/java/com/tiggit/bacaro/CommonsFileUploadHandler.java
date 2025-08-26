package com.tiggit.bacaro;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;

public class CommonsFileUploadHandler implements RequestHandler<Object, Object> {

    private static final Logger logger = LoggerFactory.getLogger(CommonsFileUploadHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final VelocityEngine velocityEngine;

    public CommonsFileUploadHandler() {
        // Initialize Velocity Engine
        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("resource.loaders", "classpath");
        velocityEngine.setProperty("resource.loader.classpath.class", ClasspathResourceLoader.class.getName());
        velocityEngine.init();
    }

    @Override
    public Object handleRequest(Object input, Context context) {
        try {
            // Parse the raw input
            String inputJson = objectMapper.writeValueAsString(input);
            JsonNode event = objectMapper.readTree(inputJson);

            logger.info("Processing request...");

            // Extract basic information
            String body = extractBody(event);
            boolean isBase64Encoded = extractIsBase64Encoded(event);
            Map<String, String> headers = extractHeaders(event);

            // Check if this is a request for an image or static resource
            String path = extractPath(event);
            if (path != null && (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".gif"))) {
                logger.info("Serving image: " + path);
                return serveImage(path);
            }

            // Check if this looks like a file upload (has body with multipart data)
            if (body != null && !body.isEmpty() && headers.containsKey("content-type")
                    && headers.get("content-type").contains("multipart/form-data")) {
                logger.info("Detected file upload request");
                return handleFileUpload(body, isBase64Encoded, headers);
            }

            // For all other requests, serve the HTML page
            logger.info("Serving HTML page");
            String html = getIndexHtml();
            return createResponse(200, html, "text/html");

        } catch (Exception e) {
            logger.error("Error processing request", e);
            return createResponse(500, "Error: " + e.getMessage(), "text/plain");
        }
    }

    private String extractBody(JsonNode event) {
        JsonNode body = event.get("body");
        return (body != null && !body.isNull()) ? body.asText() : null;
    }

    private boolean extractIsBase64Encoded(JsonNode event) {
        JsonNode encoded = event.get("isBase64Encoded");
        return encoded != null && encoded.asBoolean(false);
    }

    private Map<String, String> extractHeaders(JsonNode event) {
        Map<String, String> headers = new HashMap<>();

        JsonNode headersNode = event.get("headers");
        if (headersNode != null && headersNode.isObject()) {
            headersNode.fields().forEachRemaining(entry -> {
                headers.put(entry.getKey().toLowerCase(), entry.getValue().asText());
            });
        }

        return headers;
    }

    private String extractPath(JsonNode event) {
        // Try to extract path from various places in the event
        JsonNode path = event.get("path");
        if (path != null && !path.isNull()) {
            return path.asText();
        }

        JsonNode rawPath = event.get("rawPath");
        if (rawPath != null && !rawPath.isNull()) {
            return rawPath.asText();
        }

        return "/"; // default
    }

    private Object serveImage(String imagePath) {
        try {
            // Remove leading slash if present
            String resourcePath = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;

            // Try to load the image from classpath resources
            InputStream imageStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (imageStream == null) {
                // Try common locations
                imageStream = getClass().getClassLoader().getResourceAsStream("static/" + resourcePath);
                if (imageStream == null) {
                    imageStream = getClass().getClassLoader().getResourceAsStream("images/" + resourcePath);
                }
            }

            if (imageStream == null) {
                logger.warn("Image not found: " + imagePath);
                return createResponse(404, "Image not found", "text/plain");
            }

            // Read image bytes
            byte[] imageBytes = imageStream.readAllBytes();
            String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);

            // Determine content type
            String contentType = "image/png"; // default
            if (imagePath.toLowerCase().endsWith(".jpg") || imagePath.toLowerCase().endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (imagePath.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            }

            // Create response with base64 encoded image
            Map<String, Object> response = new HashMap<>();
            response.put("statusCode", 200);
            response.put("body", base64Image);
            response.put("isBase64Encoded", true);

            Map<String, String> responseHeaders = new HashMap<>();
            responseHeaders.put("Content-Type", contentType);
            responseHeaders.put("Cache-Control", "public, max-age=31536000"); // Cache for 1 year
            response.put("headers", responseHeaders);

            logger.info("Served image: " + imagePath + " (" + imageBytes.length + " bytes)");
            return response;

        } catch (Exception e) {
            logger.error("Error serving image: " + imagePath, e);
            return createResponse(500, "Error serving image", "text/plain");
        }
    }

    // Simple wrapper to make our Lambda event look like an HttpServletRequest for Commons FileUpload
    private static class MockHttpServletRequest implements HttpServletRequest {
        private final byte[] body;
        private final String contentType;

        public MockHttpServletRequest(byte[] body, String contentType) {
            this.body = body;
            this.contentType = contentType;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }

        @Override
        public javax.servlet.ServletInputStream getInputStream() throws java.io.IOException {
            return new javax.servlet.ServletInputStream() {
                private final ByteArrayInputStream stream = new ByteArrayInputStream(body);

                @Override
                public int read() throws java.io.IOException {
                    return stream.read();
                }

                @Override
                public boolean isFinished() {
                    return stream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(javax.servlet.ReadListener readListener) {
                    // Not implemented for this mock
                }
            };
        }

        // Minimal required implementations - most return defaults
        @Override public String getMethod() { return "POST"; }
        @Override public String getRequestURI() { return "/"; }
        @Override public StringBuffer getRequestURL() { return new StringBuffer("http://localhost/"); }
        @Override public String getServletPath() { return "/"; }
        @Override public String getQueryString() { return null; }
        @Override public String getProtocol() { return "HTTP/1.1"; }
        @Override public String getScheme() { return "http"; }
        @Override public String getServerName() { return "localhost"; }
        @Override public int getServerPort() { return 80; }
        @Override public String getRemoteAddr() { return "127.0.0.1"; }
        @Override public String getRemoteHost() { return "localhost"; }
        @Override public String getCharacterEncoding() { return "UTF-8"; }
        @Override public void setCharacterEncoding(String env) {}
        @Override public java.util.Locale getLocale() { return java.util.Locale.getDefault(); }
        @Override public java.util.Enumeration<java.util.Locale> getLocales() { return java.util.Collections.enumeration(java.util.Arrays.asList(java.util.Locale.getDefault())); }
        @Override public boolean isSecure() { return false; }
        @Override public String getAuthType() { return null; }
        @Override public javax.servlet.http.Cookie[] getCookies() { return null; }
        @Override public long getDateHeader(String name) { return -1; }
        @Override public String getHeader(String name) { return "content-type".equals(name.toLowerCase()) ? contentType : null; }
        @Override public java.util.Enumeration<String> getHeaders(String name) { return java.util.Collections.enumeration(java.util.Arrays.asList(getHeader(name))); }
        @Override public java.util.Enumeration<String> getHeaderNames() { return java.util.Collections.enumeration(java.util.Arrays.asList("content-type")); }
        @Override public int getIntHeader(String name) { return -1; }
        @Override public String getPathInfo() { return null; }
        @Override public String getPathTranslated() { return null; }
        @Override public String getContextPath() { return ""; }
        @Override public String getRemoteUser() { return null; }
        @Override public boolean isUserInRole(String role) { return false; }
        @Override public java.security.Principal getUserPrincipal() { return null; }
        @Override public String getRequestedSessionId() { return null; }
        @Override public javax.servlet.http.HttpSession getSession(boolean create) { return null; }
        @Override public javax.servlet.http.HttpSession getSession() { return null; }
        @Override public String changeSessionId() { return null; }
        @Override public boolean isRequestedSessionIdValid() { return false; }
        @Override public boolean isRequestedSessionIdFromCookie() { return false; }
        @Override public boolean isRequestedSessionIdFromURL() { return false; }
        @Override public boolean isRequestedSessionIdFromUrl() { return false; }
        @Override public boolean authenticate(javax.servlet.http.HttpServletResponse response) { return false; }
        @Override public void login(String username, String password) {}
        @Override public void logout() {}
        @Override public java.util.Collection<javax.servlet.http.Part> getParts() { return null; }
        @Override public javax.servlet.http.Part getPart(String name) { return null; }
        @Override public <T extends javax.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }
        @Override public Object getAttribute(String name) { return null; }
        @Override public java.util.Enumeration<String> getAttributeNames() { return java.util.Collections.emptyEnumeration(); }
        @Override public void setAttribute(String name, Object o) {}
        @Override public void removeAttribute(String name) {}
        @Override public javax.servlet.RequestDispatcher getRequestDispatcher(String path) { return null; }
        @Override public String getRealPath(String path) { return null; }
        @Override public int getRemotePort() { return 0; }
        @Override public String getLocalName() { return "localhost"; }
        @Override public String getLocalAddr() { return "127.0.0.1"; }
        @Override public int getLocalPort() { return 80; }
        @Override public javax.servlet.ServletContext getServletContext() { return null; }
        @Override public javax.servlet.AsyncContext startAsync() { return null; }
        @Override public javax.servlet.AsyncContext startAsync(javax.servlet.ServletRequest servletRequest, javax.servlet.ServletResponse servletResponse) { return null; }
        @Override public boolean isAsyncStarted() { return false; }
        @Override public boolean isAsyncSupported() { return false; }
        @Override public javax.servlet.AsyncContext getAsyncContext() { return null; }
        @Override public javax.servlet.DispatcherType getDispatcherType() { return javax.servlet.DispatcherType.REQUEST; }
        @Override public String getParameter(String name) { return null; }
        @Override public java.util.Enumeration<String> getParameterNames() { return java.util.Collections.emptyEnumeration(); }
        @Override public String[] getParameterValues(String name) { return null; }
        @Override public java.util.Map<String, String[]> getParameterMap() { return java.util.Collections.emptyMap(); }
        @Override public java.io.BufferedReader getReader() { return new java.io.BufferedReader(new java.io.StringReader(new String(body))); }
    }

    private Object handleFileUpload(String body, boolean isBase64Encoded, Map<String, String> headers) {
        try {
            logger.info("Processing file upload...");
            logger.info("Body length: {}, Base64: {}", body != null ? body.length() : 0, isBase64Encoded);
            logger.info("Content-Type: {}", headers.get("content-type"));

            if (body == null || body.isEmpty()) {
                logger.error("No body data received");
                return createResponse(400, "No file data received", "text/plain");
            }

            byte[] bodyBytes;
            if (isBase64Encoded) {
                bodyBytes = Base64.getDecoder().decode(body);
            } else {
                bodyBytes = body.getBytes();
            }

            logger.info("Body bytes length: {}", bodyBytes.length);

            // Use Apache Commons FileUpload
            DiskFileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);

            MockHttpServletRequest request = new MockHttpServletRequest(bodyBytes, headers.get("content-type"));
            List<FileItem> items = upload.parseRequest(request);

            logger.info("Found {} file items", items.size());

            FileItem fileItem = null;
            for (FileItem item : items) {
                if (!item.isFormField()) {
                    fileItem = item;
                    break;
                }
            }

            if (fileItem == null) {
                return createResponse(400, "No file found in upload", "text/plain");
            }

            logger.info("File found: {}, Size: {}", fileItem.getName(), fileItem.getSize());

            // Get the file input stream
            InputStream fileStream = fileItem.getInputStream();

            // Process the spreadsheet
            SpreadSheetReader reader = new SpreadSheetReader();
            List<StaffPayment> payments = reader.read(fileStream);

            logger.info("Processed {} payments", payments.size());

            if (payments.isEmpty()) {
                logger.warn("No payments found in spreadsheet");
                return createResponse(200, "<html><body><h2>No payments found in the uploaded file</h2></body></html>", "text/html");
            }

            // Create Velocity context
            VelocityContext velocityContext = new VelocityContext();
            velocityContext.put("payments", payments);
            velocityContext.put("date", new DateTool());
            velocityContext.put("number", new NumberTool());

            // Process template
            Template template = velocityEngine.getTemplate("templates/payments.vm");
            StringWriter writer = new StringWriter();
            template.merge(velocityContext, writer);
            String result = writer.toString();

            logger.info("Template processed successfully, result length: {}", result.length());

            return createResponse(200, result, "text/html");

        } catch (FileUploadException e) {
            logger.error("FileUpload error", e);
            String errorHtml = "<html><body><h2>File Upload Error</h2><p>" + e.getMessage() + "</p></body></html>";
            return createResponse(500, errorHtml, "text/html");
        } catch (Exception e) {
            logger.error("Error processing file upload", e);
            String errorHtml = "<html><body><h2>Error Processing File</h2><p>" + e.getMessage() + "</p><pre>" + getStackTrace(e) + "</pre></body></html>";
            return createResponse(500, errorHtml, "text/html");
        }
    }

    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    private Object createResponse(int statusCode, String body, String contentType) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);
        response.put("body", body);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", contentType + "; charset=utf-8");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type");
        response.put("headers", headers);

        return response;
    }

    private String getIndexHtml() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <style>\n" +
                "        @import url('https://fonts.googleapis.com/css?family=Miriam+Libre&display=swap');\n" +
                "        \n" +
                "        body {\n" +
                "            font-family: 'Miriam Libre', serif;\n" +
                "            margin: 0;\n" +
                "            padding: 20px;\n" +
                "            background-color: #f5f5f5;\n" +
                "        }\n" +
                "        \n" +
                "        .logo {\n" +
                "            max-width: 200px;\n" +
                "            height: auto;\n" +
                "        }\n" +
                "        \n" +
                "        h2 {\n" +
                "            color: #333;\n" +
                "            margin: 20px 0;\n" +
                "        }\n" +
                "        \n" +
                "        .text {\n" +
                "            margin: 20px 0;\n" +
                "            color: #666;\n" +
                "            font-size: 16px;\n" +
                "        }\n" +
                "        \n" +
                "        .dropZone {\n" +
                "            width: 400px;\n" +
                "            height: 200px;\n" +
                "            border: 3px dashed #ccc;\n" +
                "            border-radius: 10px;\n" +
                "            text-align: center;\n" +
                "            line-height: 200px;\n" +
                "            font-size: 18px;\n" +
                "            color: #999;\n" +
                "            cursor: pointer;\n" +
                "            transition: all 0.3s ease;\n" +
                "            margin: 20px 0;\n" +
                "        }\n" +
                "        \n" +
                "        .dropZone:hover {\n" +
                "            border-color: #999;\n" +
                "            background-color: #fafafa;\n" +
                "        }\n" +
                "        \n" +
                "        .dropZone.dragover {\n" +
                "            border-color: #007bff;\n" +
                "            background-color: #e3f2fd;\n" +
                "            color: #007bff;\n" +
                "        }\n" +
                "        \n" +
                "        .hidden {\n" +
                "            display: none !important;\n" +
                "        }\n" +
                "        \n" +
                "        #status {\n" +
                "            margin: 20px 0;\n" +
                "            padding: 10px;\n" +
                "            border-radius: 5px;\n" +
                "            display: none;\n" +
                "        }\n" +
                "        \n" +
                "        .success {\n" +
                "            background-color: #d4edda;\n" +
                "            color: #155724;\n" +
                "            border: 1px solid #c3e6cb;\n" +
                "        }\n" +
                "        \n" +
                "        .error {\n" +
                "            background-color: #f8d7da;\n" +
                "            color: #721c24;\n" +
                "            border: 1px solid #f5c6cb;\n" +
                "        }\n" +
                "        \n" +
                "        .processing {\n" +
                "            background-color: #d1ecf1;\n" +
                "            color: #0c5460;\n" +
                "            border: 1px solid #bee5eb;\n" +
                "        }\n" +
                "    </style>\n" +
                "    \n" +
                "    <title>Bacaro Grats Manager</title>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "</head>\n" +
                "<body>\n" +
                "    <img src=\"data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KICAgIDx0ZXh0IHg9IjEwIiB5PSI1MCIgZm9udC1mYW1pbHk9IkFyaWFsLCBzYW5zLXNlcmlmIiBmb250LXNpemU9IjI0IiBmaWxsPSIjMzMzIj5CYWNhcm8gR3JhdHM8L3RleHQ+Cjwvc3ZnPgo=\" class=\"logo\" alt=\"Bacaro Grats Logo\" />\n" +
                "    \n" +
                "    <h2>Grats spreadsheet printing tool</h2>\n" +
                "    <div class=\"text\">Drop a spreadsheet from the accountant in the box below</div>\n" +
                "    <div class=\"dropZone\" id=\"dropZone\">Drop your spreadsheet here or click to select</div>\n" +
                "    \n" +
                "    <div id=\"status\"></div>\n" +
                "    \n" +
                "    <div class=\"hidden\">\n" +
                "        <form method=\"post\" action=\".\" enctype=\"multipart/form-data\" id=\"postForm\">\n" +
                "            <input type=\"file\" id=\"fileInput\" name=\"file\" accept=\".xlsx,.xls,.csv\"/>\n" +
                "            <input type=\"submit\" value=\"submit\" id=\"submit\" />\n" +
                "        </form>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div id=\"results\"></div>\n" +
                "    \n" +
                "    <script>\n" +
                "        const dropZone = document.getElementById('dropZone');\n" +
                "        const fileInput = document.getElementById('fileInput');\n" +
                "        const status = document.getElementById('status');\n" +
                "        const results = document.getElementById('results');\n" +
                "        \n" +
                "        // Click to select file\n" +
                "        dropZone.addEventListener('click', function() {\n" +
                "            fileInput.click();\n" +
                "        });\n" +
                "        \n" +
                "        // File input change\n" +
                "        fileInput.addEventListener('change', function(event) {\n" +
                "            if (event.target.files.length > 0) {\n" +
                "                handleFile(event.target.files[0]);\n" +
                "            }\n" +
                "        });\n" +
                "        \n" +
                "        // Drag and drop\n" +
                "        dropZone.addEventListener('dragover', function(event) {\n" +
                "            event.preventDefault();\n" +
                "            dropZone.classList.add('dragover');\n" +
                "        });\n" +
                "        \n" +
                "        dropZone.addEventListener('dragleave', function() {\n" +
                "            dropZone.classList.remove('dragover');\n" +
                "        });\n" +
                "        \n" +
                "        dropZone.addEventListener('drop', function(event) {\n" +
                "            event.preventDefault();\n" +
                "            dropZone.classList.remove('dragover');\n" +
                "            \n" +
                "            const files = event.dataTransfer.files;\n" +
                "            if (files.length > 0) {\n" +
                "                handleFile(files[0]);\n" +
                "            }\n" +
                "        });\n" +
                "        \n" +
                "        function showStatus(message, type) {\n" +
                "            status.textContent = message;\n" +
                "            status.className = type;\n" +
                "            status.style.display = 'block';\n" +
                "        }\n" +
                "        \n" +
                "        function hideStatus() {\n" +
                "            status.style.display = 'none';\n" +
                "        }\n" +
                "        \n" +
                "        function handleFile(file) {\n" +
                "            showStatus('Processing file...', 'processing');\n" +
                "            results.innerHTML = '';\n" +
                "            \n" +
                "            const formData = new FormData();\n" +
                "            formData.append('file', file);\n" +
                "            \n" +
                "            // Post to the same endpoint (API Gateway will route to same Lambda)\n" +
                "            fetch(window.location.href, {\n" +
                "                method: 'POST',\n" +
                "                body: formData\n" +
                "            })\n" +
                "            .then(function(response) {\n" +
                "                if (response.ok) {\n" +
                "                    return response.text();\n" +
                "                } else {\n" +
                "                    throw new Error('Server error: ' + response.status);\n" +
                "                }\n" +
                "            })\n" +
                "            .then(function(result) {\n" +
                "                // Hide the entire upload interface\n" +
                "                document.querySelector('.logo').style.display = 'none';\n" +
                "                document.querySelector('h2').style.display = 'none';\n" +
                "                document.querySelector('.text').style.display = 'none';\n" +
                "                dropZone.style.display = 'none';\n" +
                "                document.getElementById('status').style.display = 'none';\n" +
                "                \n" +
                "                // Add print-specific CSS for page breaks\n" +
                "                var printStyles = document.createElement('style');\n" +
                "                printStyles.textContent = `\n" +
                "                    @media print {\n" +
                "                        .employeePage {\n" +
                "                            page-break-after: always;\n" +
                "                            page-break-inside: avoid;\n" +
                "                        }\n" +
                "                        .employeePage:last-child {\n" +
                "                            page-break-after: auto;\n" +
                "                        }\n" +
                "                        body {\n" +
                "                            font-size: 12pt;\n" +
                "                            line-height: 1.4;\n" +
                "                        }\n" +
                "                        h1, h2, h3 {\n" +
                "                            page-break-after: avoid;\n" +
                "                        }\n" +
                "                    }\n" +
                "                `;\n" +
                "                document.head.appendChild(printStyles);\n" +
                "                \n" +
                "                results.innerHTML = result;\n" +
                "            })\n" +
                "            .catch(function(error) {\n" +
                "                showStatus('Error: ' + error.message, 'error');\n" +
                "            });\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}