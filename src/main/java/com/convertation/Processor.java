package com.convertation;

import java.io.BufferedReader;
import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Processor {
    public static LoggerToFile logger;
    private Map<String, CompanyStructure> holders;

    Processor(File logFile) throws CustomException {
        holders = new HashMap<>();
        logger = new LoggerToFile(logFile, Processor.class.getName());
    }

    public void setHolders(Map<String, CompanyStructure> holders) {
        this.holders = holders;
    }

    public Map<String, CompanyStructure> getHolders() {
        return holders;
    }

    public static File createFile(String fileName) throws CustomException {
        try {
            return new File(System.getProperty("user.dir") + "\\src\\" + fileName);
        } catch (Exception e) {
            throw new CustomException("File creation failed.");
        }
    }

    public Map<String, CompanyStructure> deserializeToListOfCompanyStructures(BufferedReader in)
            throws CustomException {
        try {
            return in.lines().map(line -> {
                String[] x = setPattern().split(line);
                return new CompanyStructure(x[0], x[1], LocalDate.parse(x[2]), x[3], LocalDate.parse(x[4]),
                        Integer.parseInt(x[5]), x[6], x[7], x[8], x[9], x[10], x[11]);
            })
                    .filter(s -> !s.getShortName().isEmpty() || !s.getBranchOfWork().isEmpty() ||
                            !s.getTypeOfWork().isEmpty())
                    .collect(Collectors.toMap(CompanyStructure::getShortNameToLowerCase, Function.identity()));
        } catch (Exception ex) {
            throw new CustomException("Parsing file failed.");
        }
    }

    public static boolean checkOutfileForValidity(String filename, int counterOfQueries) {
        return filename.equalsIgnoreCase("request" + counterOfQueries);
    }

    public List<String> processCommand(CommandName flag, List<String> rawParams, int counterOfQueries) {
        List<String> inputInfo = new ArrayList<>();
        if (flag == CommandName.WHERE) {
            inputInfo = WHERECommand(rawParams, counterOfQueries);
        } else if (flag == CommandName.INTO) {
            String temp = INTOCommand(rawParams, counterOfQueries);
            inputInfo.add(temp);
        } else if (flag == CommandName.FROM) {
            if (!FROMCommand(rawParams)) {
                throw new IllegalStateException("Invalid source table directory in " + counterOfQueries + " query.");
            }
        }
        return inputInfo;
    }

    private boolean FROMCommand(List<String> rawParams) {
        return rawParams.size() == 1 && rawParams.get(0).equalsIgnoreCase("company_table");
    }

    private StringBuilder rawParamsProcessing(List<String> rawParams, StringBuilder inputInfo) {
        if (rawParams.size() == 3) {
            inputInfo = new StringBuilder(rawParams.get(2).substring(1, rawParams.get(2).length() - 1));
        } else if (rawParams.size() > 3) {
            inputInfo.append(rawParams.get(2), 1, rawParams.get(2).length()).append(" ");
            for (int i = 3; i < rawParams.size() - 1; i++) {
                inputInfo.append(rawParams.get(i)).append(" ");
            }
            inputInfo.append(rawParams.get(rawParams.size() - 1), 0,
                    rawParams.get(rawParams.size() - 1).length() - 1);
        }
        return inputInfo;
    }

    private boolean ifNumEmployeesCommand(List<String> rawParams) {
        return rawParams.get(0).equalsIgnoreCase("number_of_employees") && rawParams.get(1).equals(">=")
                && rawParams.get(3).equalsIgnoreCase("AND") && rawParams.get(4).equalsIgnoreCase(rawParams.get(0))
                && rawParams.get(5).equals("<=");
    }

    private List<String> WHERECommand(List<String> rawParams, int counterOfQueries) {
        String requestNum;
        StringBuilder inputInfo = new StringBuilder();
        if (rawParams.get(0).equalsIgnoreCase("short_name") && rawParams.get(1).equals("=")) {
            requestNum = RequestName.SHORT_NAME.toString();
            inputInfo = rawParamsProcessing(rawParams, inputInfo);
        } else if (rawParams.get(0).equalsIgnoreCase("type_of_work") && rawParams.get(1).equals("=")) {
            requestNum = RequestName.TYPE_OF_WORK.toString();
            inputInfo = rawParamsProcessing(rawParams, inputInfo);
        } else if (rawParams.size() == 7 && ifNumEmployeesCommand(rawParams)) {
            requestNum = RequestName.EMPLOYEES.toString();
            inputInfo = new StringBuilder(rawParams.get(2) + " " + rawParams.get(6));
        } else {
            throw new IllegalStateException("Invalid request type in " + counterOfQueries + " query.");
        }
        return new ArrayList<>(Arrays.asList(requestNum, inputInfo.toString()));
    }

    private String INTOCommand(List<String> rawParams, int counterOfQueries) {
        String filepath;
        if (rawParams.size() == 2) {
            filepath = rawParams.get(1).substring(1, rawParams.get(1).length() - 2);
        } else {
            throw new IllegalStateException("Invalid filepath name in " + counterOfQueries + " query.");
        }
        return filepath;
    }

    private Pattern setPattern() {
        return Pattern.compile(",");
    }

    CompanyStructure searchByShortName(String shortName) {
        return holders.get(shortName);
    }

    List<CompanyStructure> searchByTypeOfWork(String typeOfWork) {
        return holders
                .values()
                .stream()
                .filter(sh -> sh.getTypeOfWork().equalsIgnoreCase(typeOfWork))
                .collect(Collectors.toList());
    }

    List<CompanyStructure> searchByEmployees(int lower, int upper) {
        return holders
                .values()
                .stream()
                .filter(sh -> sh.getNumberOfEmployees() >= lower && sh.getNumberOfEmployees() <= upper)
                .collect(Collectors.toList());
    }
}
