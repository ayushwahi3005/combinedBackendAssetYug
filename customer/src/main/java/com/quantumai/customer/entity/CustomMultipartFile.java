package com.quantumai.customer.entity;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class CustomMultipartFile implements MultipartFile {

    private final MultipartFile original;
    private final String newFileName;

    public CustomMultipartFile(MultipartFile original, String newFileName) {
        this.original = original;
        this.newFileName = newFileName;
    }

    @Override
    public String getName() {
        return original.getName();
    }

    @Override
    public String getOriginalFilename() {
        return newFileName;
    }

    @Override
    public String getContentType() {
        return original.getContentType();
    }

    @Override
    public boolean isEmpty() {
        return original.isEmpty();
    }

    @Override
    public long getSize() {
        return original.getSize();
    }

    @Override
    public byte[] getBytes() throws IOException {
        return original.getBytes();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return original.getInputStream();
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        original.transferTo(dest);
    }
}