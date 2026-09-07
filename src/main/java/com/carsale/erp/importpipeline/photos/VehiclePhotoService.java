package com.carsale.erp.importpipeline.photos;

import com.carsale.erp.shared.document.SheetDocumentStorageService;
import com.carsale.erp.shared.vehicle.Vehicle;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carsale.erp.shared.document.SheetUploadResult;
import com.carsale.erp.importpipeline.photos.VehiclePhoto;
import com.carsale.erp.importpipeline.photos.VehiclePhotoRepository;
import com.carsale.erp.shared.vehicle.VehicleRepository;

@Service
public class VehiclePhotoService {

    public static final int MAX_PHOTOS = 5;

    private final VehiclePhotoRepository photoRepository;
    private final VehicleRepository vehicleRepository;
    private final SheetDocumentStorageService documentStorageService;

    public VehiclePhotoService(
            VehiclePhotoRepository photoRepository,
            VehicleRepository vehicleRepository,
            SheetDocumentStorageService documentStorageService
    ) {
        this.photoRepository = photoRepository;
        this.vehicleRepository = vehicleRepository;
        this.documentStorageService = documentStorageService;
    }

    public List<VehiclePhoto> list(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return photoRepository.findByChassisNoOrderBySortOrderAscIdAsc(chassisNo.trim());
    }

    public boolean hasPhotos(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return false;
        }
        return photoRepository.countByChassisNo(chassisNo.trim()) > 0;
    }

    public int count(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return 0;
        }
        return (int) photoRepository.countByChassisNo(chassisNo.trim());
    }

    @Transactional
    public VehiclePhoto add(String chassisNo, SheetUploadResult stored) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        String trimmed = chassisNo.trim();
        if (!vehicleRepository.existsById(trimmed)) {
            throw new IllegalArgumentException("Vehicle not found for chassis " + trimmed);
        }
        if (stored == null || stored.getStoredName() == null || stored.getStoredName().trim().isEmpty()) {
            throw new IllegalArgumentException("Please choose a vehicle image.");
        }
        int current = (int) photoRepository.countByChassisNo(trimmed);
        if (current >= MAX_PHOTOS) {
            documentStorageService.deletePhotoIfExists(stored.getStoredName());
            throw new IllegalArgumentException("A vehicle can have at most " + MAX_PHOTOS + " images.");
        }
        VehiclePhoto photo = new VehiclePhoto();
        photo.setChassisNo(trimmed);
        photo.setSortOrder(current + 1);
        photo.setOriginalName(stored.getOriginalName());
        photo.setStoredName(stored.getStoredName());
        photo.setContentType(stored.getContentType());
        return photoRepository.save(photo);
    }

    @Transactional
    public void delete(String chassisNo, Long photoId) {
        if (chassisNo == null || photoId == null) {
            return;
        }
        VehiclePhoto photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null || !chassisNo.trim().equals(photo.getChassisNo())) {
            return;
        }
        documentStorageService.deletePhotoIfExists(photo.getStoredName());
        photoRepository.delete(photo);
        resequence(chassisNo.trim());
    }

    @Transactional
    public void deleteAll(String chassisNo) {
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            return;
        }
        List<VehiclePhoto> photos = list(chassisNo);
        for (int i = 0; i < photos.size(); i++) {
            documentStorageService.deletePhotoIfExists(photos.get(i).getStoredName());
        }
        photoRepository.deleteByChassisNo(chassisNo.trim());
    }

    @Transactional
    public void attachPending(String chassisNo, List<String> storedNames, List<String> originalNames, List<String> contentTypes) {
        if (chassisNo == null || chassisNo.trim().isEmpty() || storedNames == null) {
            return;
        }
        String trimmed = chassisNo.trim();
        for (int i = 0; i < storedNames.size() && count(trimmed) < MAX_PHOTOS; i++) {
            String storedName = storedNames.get(i);
            if (storedName == null || storedName.trim().isEmpty()) {
                continue;
            }
            SheetUploadResult stored = new SheetUploadResult();
            stored.setStoredName(storedName.trim());
            stored.setOriginalName(nameAt(originalNames, i, storedName));
            stored.setContentType(nameAt(contentTypes, i, "image/jpeg"));
            add(trimmed, stored);
        }
    }

    private void resequence(String chassisNo) {
        List<VehiclePhoto> photos = photoRepository.findByChassisNoOrderBySortOrderAscIdAsc(chassisNo);
        for (int i = 0; i < photos.size(); i++) {
            photos.get(i).setSortOrder(i + 1);
            photoRepository.save(photos.get(i));
        }
    }

    private String nameAt(List<String> values, int index, String fallback) {
        if (values == null || index >= values.size() || values.get(index) == null || values.get(index).trim().isEmpty()) {
            return fallback;
        }
        return values.get(index).trim();
    }
}
