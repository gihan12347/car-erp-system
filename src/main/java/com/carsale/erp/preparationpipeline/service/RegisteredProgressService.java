package com.carsale.erp.preparationpipeline.service;

import com.carsale.erp.importpipeline.service.VehiclePhotoService;
import com.carsale.erp.shared.service.SkippedStageService;
import com.carsale.erp.shared.vehicle.Vehicle;
import com.carsale.erp.shared.vehicle.VehicleIntake;
import com.carsale.erp.shared.vehicle.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class RegisteredProgressService {

    private static final List<String> REGISTERED_KEYS = Arrays.asList(
            SkippedStageService.REG_BOOK,
            SkippedStageService.REG_PHOTOS,
            SkippedStageService.REG_OWNER
    );

    private final RegistrationCertificateService registrationCertificateService;
    private final VehiclePhotoService vehiclePhotoService;
    private final PreviousOwnerService previousOwnerService;
    private final SkippedStageService skippedStageService;
    private final VehicleRepository vehicleRepository;

    public RegisteredProgressService(
            RegistrationCertificateService registrationCertificateService,
            VehiclePhotoService vehiclePhotoService,
            PreviousOwnerService previousOwnerService,
            SkippedStageService skippedStageService,
            VehicleRepository vehicleRepository
    ) {
        this.registrationCertificateService = registrationCertificateService;
        this.vehiclePhotoService = vehiclePhotoService;
        this.previousOwnerService = previousOwnerService;
        this.skippedStageService = skippedStageService;
        this.vehicleRepository = vehicleRepository;
    }

    public boolean isBookFilled(String chassisNo) {
        return registrationCertificateService.findByChassisNo(chassisNo) != null;
    }

    public boolean isPhotosFilled(String chassisNo) {
        return vehiclePhotoService.count(chassisNo) > 0;
    }

    public boolean isOwnerFilled(String chassisNo) {
        return previousOwnerService.isSaved(chassisNo);
    }

    public boolean isBookSkipped(String chassisNo) {
        return !isBookFilled(chassisNo) && skippedStageService.isMarked(chassisNo, SkippedStageService.REG_BOOK);
    }

    public boolean isPhotosSkipped(String chassisNo) {
        return !isPhotosFilled(chassisNo) && skippedStageService.isMarked(chassisNo, SkippedStageService.REG_PHOTOS);
    }

    public boolean isOwnerSkipped(String chassisNo) {
        return !isOwnerFilled(chassisNo) && skippedStageService.isMarked(chassisNo, SkippedStageService.REG_OWNER);
    }

    public boolean isBookReady(String chassisNo) {
        return isBookFilled(chassisNo) || isBookSkipped(chassisNo);
    }

    public boolean isPhotosReady(String chassisNo) {
        return isPhotosFilled(chassisNo) || isPhotosSkipped(chassisNo);
    }

    public boolean isOwnerReady(String chassisNo) {
        return isOwnerFilled(chassisNo) || isOwnerSkipped(chassisNo);
    }

    public boolean isStageSatisfied(String chassisNo, String stageKey) {
        if (SkippedStageService.REG_BOOK.equals(stageKey)) {
            return isBookReady(chassisNo);
        }
        if (SkippedStageService.REG_PHOTOS.equals(stageKey)) {
            return isPhotosReady(chassisNo);
        }
        if (SkippedStageService.REG_OWNER.equals(stageKey)) {
            return isOwnerReady(chassisNo);
        }
        return false;
    }

    @Transactional
    public void skipStage(String chassisNo, String stageKey) {
        String key = SkippedStageService.canonical(stageKey);
        if (key == null || !REGISTERED_KEYS.contains(key)) {
            throw new IllegalArgumentException("That stage cannot be skipped.");
        }
        if (chassisNo == null || chassisNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Chassis number is required.");
        }
        Vehicle vehicle = vehicleRepository.findById(chassisNo.trim())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
        if (vehicle.getIntake() != VehicleIntake.LOCAL_REGISTERED) {
            throw new IllegalArgumentException("That stage cannot be skipped.");
        }
        if (isStageFilled(vehicle.getChassisNo(), key)) {
            return;
        }
        skippedStageService.add(vehicle.getChassisNo(), key);
    }

    private boolean isStageFilled(String chassisNo, String stageKey) {
        if (SkippedStageService.REG_BOOK.equals(stageKey)) {
            return isBookFilled(chassisNo);
        }
        if (SkippedStageService.REG_PHOTOS.equals(stageKey)) {
            return isPhotosFilled(chassisNo);
        }
        return SkippedStageService.REG_OWNER.equals(stageKey) && isOwnerFilled(chassisNo);
    }

    public boolean isComplete(String chassisNo) {
        return isBookReady(chassisNo) && isPhotosReady(chassisNo) && isOwnerReady(chassisNo);
    }

    public boolean readyForPreparation(Vehicle vehicle) {
        if (vehicle == null || vehicle.getIntake() != VehicleIntake.LOCAL_REGISTERED) {
            return true;
        }
        return isComplete(vehicle.getChassisNo());
    }
}
