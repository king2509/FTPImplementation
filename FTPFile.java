import java.util.Map;

class FTPFile {
    private String updateDate = null;
    private String updateTime = null;
    private String type = null;
    private String filename = null;
    private long size = 0;

    FTPFile(String[] fileDetails){
        updateDate = fileDetails[0];
        updateTime = fileDetails[1];
        String typeOrSize = fileDetails[2];
        if(typeOrSize.equals("<DIR>"))
            type = "DIR";
        else {
            type = "FILE";
            size = Long.parseLong(typeOrSize);
        }
        filename = fileDetails[3];
    }

    boolean isFile() {
        return type.equals("FILE");
    }

    String getUpdateDate(){
        return updateDate;
    }

    String getUpdateTime() {
        return updateTime;
    }


    String getFilename(){
        return filename;
    }

    String getType(){
        return type;
    }

    Long getSize() throws NoSuchMethodError{
        if(isFile())
            return size;
        else {
            throw new NoSuchMethodError("DIR has no size");
        }
    }

    public String toString() {
        if(isFile())
            return updateDate + " " + updateTime + " " + type + " " + filename + " " + size;
        return updateDate + " " + updateTime + " " + type + " " + filename;
    }
}
