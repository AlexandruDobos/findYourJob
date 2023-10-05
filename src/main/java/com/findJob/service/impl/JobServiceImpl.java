package com.findJob.service.impl;

import com.findJob.dto.FeedbackDTO;
import com.findJob.dto.JobDTO;
import com.findJob.dto.UserDTO;
import com.findJob.entity.*;
import com.findJob.exception.BadRequestException;
import com.findJob.exception.NotFoundException;
import com.findJob.repository.*;
import com.findJob.service.JobService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class JobServiceImpl implements JobService {

    private JobRepository jobRepository;
    private UserProfileRepository userProfileRepository;
    private UserProfileJobRepository userProfileJobRepository;
    private UserRepository userRepository;
    private ModelMapper modelMapper;
    private EmployerProfileRepository employerProfileRepository;

    public JobServiceImpl(JobRepository jobRepository, UserProfileRepository userProfileRepository, UserProfileJobRepository userProfileJobRepository, UserRepository userRepository, ModelMapper modelMapper, EmployerProfileRepository employerProfileRepository) {
        this.jobRepository = jobRepository;
        this.userProfileRepository = userProfileRepository;
        this.userProfileJobRepository = userProfileJobRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.employerProfileRepository = employerProfileRepository;
    }

    @Override
    public JobDTO createJob(Integer employerProfileId, JobDTO jobDTO) throws NotFoundException {

        Optional<EmployerProfile> employerProfileOptional = employerProfileRepository.findById(employerProfileId);
        EmployerProfile employerProfile = employerProfileOptional.orElseThrow(() -> new NotFoundException("Profile not found!"));

        Job job = modelMapper.map(jobDTO, Job.class);
        job.setEmployerProfile(employerProfile);
        job.setIsActive(true);
        job.setReports(0);
        job.setDate(LocalDate.now());
        jobRepository.save(job);

        return modelMapper.map(job, JobDTO.class);
    }

    @Override
    public JobDTO getJob(Integer jobId) throws NotFoundException {

        Optional<Job> jobOptional = jobRepository.findById(jobId);
        Job job = jobOptional.orElseThrow(() -> new NotFoundException("Job not found!"));

        Integer noOfCandidates = job.getCandidates().size();

        JobDTO jobDTO = modelMapper.map(job, JobDTO.class);
        jobDTO.setNoOfCandidates(noOfCandidates);

        return jobDTO;
    }

    @Override
    public JobDTO updateJob(Integer jobId, JobDTO jobDTO) throws NotFoundException {

        Optional<Job> jobOptional = jobRepository.findById(jobId);
        Job job = jobOptional.orElseThrow(() -> new NotFoundException("Job not found!"));

        if(jobDTO.getTitle() != null) job.setTitle(jobDTO.getTitle());
        if(jobDTO.getExperienceLevel() != null) job.setExperienceLevel(jobDTO.getExperienceLevel());
        if(jobDTO.getLocationType() != null) job.setLocationType(jobDTO.getLocationType());
        if(jobDTO.getAddress() != null) job.setAddress(jobDTO.getAddress());
        if(jobDTO.getEmploymentType() != null) job.setEmploymentType(jobDTO.getEmploymentType());
        if(jobDTO.getSkills() != null) job.setSkills(jobDTO.getSkills());
        if(jobDTO.getDescription() != null) job.setDescription(jobDTO.getDescription());
        if(jobDTO.getIsActive() != null) {
            job.setIsActive(jobDTO.getIsActive());
            if(!jobDTO.getIsActive()) {
                for(UserProfileJob userProfileJob : job.getCandidates()){
                    userProfileJobRepository.delete(userProfileJob);
                }
            }
        }

        return modelMapper.map(job, JobDTO.class);
    }

    @Override
    public JobDTO applyToJob(Integer userProfileId, Integer jobId) throws NotFoundException {

        Optional<UserProfile> userProfileOptional = userProfileRepository.findById(userProfileId);
        UserProfile userProfile = userProfileOptional.orElseThrow(() -> new NotFoundException("Profile not found!"));

        Optional<Job> jobOptional = jobRepository.findById(jobId);
        Job job = jobOptional.orElseThrow(() -> new NotFoundException("Job not found!"));

        UserProfileJob userProfileJob = new UserProfileJob();
        userProfileJob.setUserProfile(userProfile);
        userProfileJob.setJob(job);

        userProfileJobRepository.save(userProfileJob);

        job.getCandidates().add(userProfileJob);
        Integer noOfCandidates = job.getCandidates().size();

        JobDTO jobDTO = modelMapper.map(job, JobDTO.class);
        jobDTO.setNoOfCandidates(noOfCandidates);

        return jobDTO;
    }

    @Override
    public JobDTO reportJob(Integer jobId, Integer userId) throws NotFoundException {

        Optional<Job> jobOptional = jobRepository.findById(jobId);
        Job job = jobOptional.orElseThrow(() -> new NotFoundException("Not found!"));

        job.setReports(job.getReports() + 1);

        if(job.getUserReportList().contains(userId))
            throw new NotFoundException("This job has already been reported!");
        else job.getUserReportList().add(userId);

        return modelMapper.map(job, JobDTO.class);

    }

    @Override
    public List<JobDTO> getAllJobReported() throws NotFoundException {

        List<Job> jobList = jobRepository.getAllJobReported();

        List<JobDTO> jobDTOList = new ArrayList<>();

        for (Job j : jobList) {

            JobDTO jobDTO = modelMapper.map(j, JobDTO.class);

            List<UserDTO> userDTOS = new ArrayList<>();

            for (Integer i : j.getUserReportList()) {

                Optional<User> userOptional = userRepository.findById(i);
                User user = userOptional.orElseThrow(()->new NotFoundException("User not found!"));
                userDTOS.add(modelMapper.map(user,UserDTO.class));
            }
            jobDTO.setUserReportList(userDTOS);

            jobDTOList.add(jobDTO);
        }

        return jobDTOList;
    }


}
