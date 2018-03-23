package org.apereo.cas.mgmt.services.web;

import org.apache.commons.io.FileUtils;
import org.apereo.cas.mgmt.GitUtil;
import org.apereo.cas.mgmt.services.GitServicesManager;
import org.apereo.cas.mgmt.services.web.beans.RegisteredServiceItem;
import org.apereo.cas.mgmt.services.web.factory.ManagerFactory;
import org.apereo.cas.mgmt.services.web.factory.RepositoryFactory;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.util.DefaultRegisteredServiceJsonSerializer;
import org.apereo.cas.services.util.RegisteredServiceYamlSerializer;
import org.apereo.cas.util.DigestUtils;
import org.eclipse.jgit.diff.RawText;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;

import static java.util.stream.Collectors.toList;


@Controller
public class SubmissionController {

    private final RepositoryFactory repositoryFactory;
    private final ManagerFactory managerFactory;

    public SubmissionController(final RepositoryFactory repositoryFactory,
                                final ManagerFactory managerFactory) {
        this.repositoryFactory = repositoryFactory;
        this.managerFactory = managerFactory;
    }

    @GetMapping("getSubmissions")
    public ResponseEntity<List<RegisteredServiceItem>> getSubmissions(final HttpServletResponse response,
                                                                      final HttpServletRequest requet) throws Exception {
        final DefaultRegisteredServiceJsonSerializer serializer = new DefaultRegisteredServiceJsonSerializer();
        try (Stream<Path> stream = Files.list(Paths.get("/ucd/local/cas-mgmt-5.2/submitted"))) {
            List<RegisteredServiceItem> list = stream.map(p -> {
                final RegisteredService service = serializer.from(p.toFile());
                final RegisteredServiceItem serviceItem = new RegisteredServiceItem();
                serviceItem.setAssignedId(p.getFileName().toString());
                serviceItem.setEvalOrder(service.getEvaluationOrder());
                serviceItem.setName(service.getName());
                serviceItem.setServiceId(service.getServiceId());
                serviceItem.setDescription(DigestUtils.abbreviate(service.getDescription()));
                if (p.getFileName().toString().startsWith("edit")) {
                    serviceItem.setStatus("EDIT");
                } else {
                    serviceItem.setStatus("SUBMITTED");
                }
                return serviceItem;
            }).collect(toList());
            return new ResponseEntity<>(list, HttpStatus.OK);
        } catch (final Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("getYamlSubmission")
    public ResponseEntity<String> getYamlSubmission(final HttpServletResponse response,
                                                    final HttpServletRequest request,
                                                    @RequestParam final String id) throws Exception {
        final DefaultRegisteredServiceJsonSerializer jsonSerializer = new DefaultRegisteredServiceJsonSerializer();
        final RegisteredService svc = jsonSerializer.from(new File("/ucd/local/cas-mgmt-5.2/submitted/" + id));
        final RegisteredServiceYamlSerializer yamlSerializer = new RegisteredServiceYamlSerializer();
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        yamlSerializer.to(output, svc);
        return new ResponseEntity<String>(output.toString(), HttpStatus.OK);
    }

    @GetMapping("getJsonSubmission")
    public ResponseEntity<String> getJsonSubmission(final HttpServletRequest request,
                                                    final HttpServletResponse response,
                                                    @RequestParam final String id) throws Exception {
        final DefaultRegisteredServiceJsonSerializer serializer = new DefaultRegisteredServiceJsonSerializer();
        final RegisteredService service = serializer.from(new File("/ucd/local/cas-mgmt-5.2/submitted/" + id));
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        serializer.to(output, service);
        return new ResponseEntity<String>(output.toString(), HttpStatus.OK);
    }

    @GetMapping("deleteSubmission")
    public ResponseEntity<String> deleteSubmission(final HttpServletResponse response,
                                                   final HttpServletRequest request,
                                                   @RequestParam final String id) throws Exception {
        Files.delete(Paths.get("/ucd/local/cas-mgmt-5.2/submitted/" + id));
        return new ResponseEntity<>("Submission deleted", HttpStatus.OK);
    }

    @GetMapping("diffSubmission")
    public void diffSubmission(final HttpServletResponse response,
                                                 final HttpServletRequest request,
                                                 @RequestParam final String id) throws Exception {
        final GitUtil git = repositoryFactory.masterRepository();
        final RawText subPath = new RawText(FileUtils.readFileToByteArray(new File("/ucd/local/cas-mgmt-5.2/submitted/" + id)));
        final String[] splitSub = id.split("-");
        final RawText gitPath = new RawText(FileUtils.readFileToByteArray(new File("/ucd/local/cas-mgmt-5.2/services-repo/service-" + splitSub[1])));
        response.getOutputStream().write(git.getFormatter(subPath, gitPath));
    }

    @GetMapping("acceptSubmission")
    public ResponseEntity<String> acceptSubmission(final HttpServletResponse response,
                                                   final HttpServletRequest request,
                                                   @RequestParam final String id) throws Exception {
        final GitServicesManager manager = managerFactory.from(request, response);
        final DefaultRegisteredServiceJsonSerializer serializer = new DefaultRegisteredServiceJsonSerializer();
        final Path path = Paths.get("/ucd/local/cas-mgmt-5.2/submitted/" + id);
        final RegisteredService service = serializer.from(path.toFile());
        manager.save(service);
        Files.delete(path);
        return new ResponseEntity<>("Service Accepted", HttpStatus.OK);
    }

    @GetMapping("importSubmission")
    public ResponseEntity<RegisteredService> importSubmission(final HttpServletResponse response,
                                                              final HttpServletRequest request,
                                                              @RequestParam final String id) throws Exception {
        final DefaultRegisteredServiceJsonSerializer serializer = new DefaultRegisteredServiceJsonSerializer();
        final RegisteredService service = serializer.from(new File("/ucd/local/cas-mgmt-5.2/submitted/" + id));
        return new ResponseEntity<>(service, HttpStatus.OK);
    }
}