package com.shop.service;

import com.shop.mapper.FileMapper;
import com.shop.response.Response;
import com.shop.response.ResponseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class FileService {

    @Autowired
    private FileMapper fileMapper;

    private String savePath;

    public FileService(@Value("${file.dir-root}") String saveRootPath,
                       @Value("${file.dir-sub}") String saveSubPath) {
        createSaveFolder(saveRootPath);
        savePath = saveRootPath + saveSubPath;
        createSaveFolder(savePath);
    }

    public void productImgUpload(MultipartFile[] files, int productId){
        saveFiles(files,getFolderPath("product"),productId);
    }

    public boolean deleteFile(Long id){
        File file = new File(getFolderPath("product")+File.separator+id);
        return file.delete();
    }

    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "transactionManager", rollbackFor = Exception.class)
    public ResponseEntity<ResponseData> imgUpload(MultipartFile file){
        Long id =saveFile(file,getFolderPath("product"),0,true);
        Map map = new HashMap<>();
        map.put("imgId", (id));
        map.put("path", ("/api/v1/img/"+id));
        return Response.getNewInstance().createResponseEntity("", map);
    }

    private void saveFiles(MultipartFile[] files,String path,int id) {
        for (MultipartFile file : files) {
            saveFile(file,path,id,false);
        }
    }

    private String getFolderPath(String folderName){
        String path =savePath + File.separator + folderName;
        createSaveFolder(path);
        return path;
    }

    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "transactionManager", rollbackFor = Exception.class)
    private Long saveFile(MultipartFile file, String path, int productId, boolean contentImg){
        String fileName = file.getOriginalFilename();

        Map map = new HashMap<>();
        map.put("fileName", fileName);
        map.put("path", path);
        map.put("productId", productId);
        map.put("contentImg", contentImg);

        fileMapper.insertFile(map);

        try {
            file.transferTo(Paths.get(path + File.separator + map.get("id")));
        } catch (IOException e) {
            e.printStackTrace();
            try {
                throw new Exception();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        return (Long) map.get("id");
    }

    private void createSaveFolder(String path) {
        File saveFolder = new File(path);
        if (!saveFolder.exists()) {
            saveFolder.mkdir();
        }
    }

    public ResponseEntity<Resource> getImg(int id) {
        String path = fileMapper.getFilePath(id);
        path = path + File.separator + id;

        Path fileRoot = Paths.get(path);


        UrlResource urlResource= null;
        try {
            urlResource = new UrlResource(fileRoot.toUri());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + URLEncoder.encode(String.valueOf(id)) + "\"").body(urlResource);
    }
}
