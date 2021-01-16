package com.convertation;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        if (args.length >= 3) {
            try (Scanner queryScanner = new Scanner(Processor.createFile(args[0]));
                 BufferedReader databaseScanner = new BufferedReader(
                         new FileReader(Processor.createFile(args[1])))) {
                File logFile = Processor.createFile(args[2]);
                Processor processor = new Processor(logFile);
                processor.setHolders(processor.deserializeToListOfCompanyStructures(databaseScanner));

                if (processor.getHolders().size() != 0) {
                    String query;
                    int counterOfQueries = 0;
                    while (queryScanner.hasNext()) {
                        query = queryScanner.nextLine();
                        if (!query.isEmpty()) {
                            counterOfQueries++;
                            List<String> requestParams = parseQueries(processor, query, counterOfQueries);
                            processRequest(counterOfQueries, processor, requestParams.get(0), requestParams.get(1),
                                    requestParams.get(2));
                            Processor.logger.writeRequestDataToLogFile(RequestName.DEFAULT,
                                    "QUERY " + counterOfQueries + " HAS BEEN PROCESSED.", 0);
                        }
                    }
                    Processor.logger.writeRequestDataToLogFile(RequestName.DEFAULT,
                            "QUERY HANDLING FINISHED SUCCESSFULLY.", 0);
                } else {
                    processException("Source table file is empty.");
                }
            } catch (CustomException | IOException e) {
                System.out.println(e.getMessage());
            } catch (IllegalStateException e) {
                processException(e.getLocalizedMessage());
            }
        } else {
            System.out.println("Invalid program arguments number. Unable to find source files");
        }
    }

    public static List<String> parseQueries(Processor processor, String query, int counterOfQueries)
            throws IllegalStateException {
        List<String> queryInput = Arrays.stream(query.split("[ ]+")).collect(Collectors.toList());
        List<String> infoToParse = new ArrayList<>();
        List<String> requestParams = new ArrayList<>();
        CommandName flag = CommandName.DEFAULT;
        if (!(queryInput.size() <= 2 || (
                queryInput.get(0).equalsIgnoreCase(CommandName.SELECT.toString()) && queryInput.get(1).equals("*")))) {
            throw new IllegalStateException("SELECT statement in query " + counterOfQueries + " is invalid.");
        }

        for (int i = 2; i <= queryInput.size(); i++) {
            if (i == queryInput.size() || queryInput.get(i).equalsIgnoreCase(CommandName.FROM.toString()) ||
                    queryInput.get(i).equalsIgnoreCase(CommandName.WHERE.toString()) ||
                    queryInput.get(i).equalsIgnoreCase(CommandName.INTO.toString())) {
                List<String> params = processor.processCommand(flag, infoToParse, counterOfQueries);
                if (!(params.isEmpty())) {
                    requestParams.addAll(params);
                }
                if (i != queryInput.size()) {
                    flag = CommandName.castFromStringToEnum(queryInput.get(i));
                }
                infoToParse.clear();
            } else {
                if (flag == CommandName.DEFAULT) {
                    throw new IllegalStateException("Unresolved symbols in " + counterOfQueries + " query.");
                } else {
                    infoToParse.add(queryInput.get(i));
                }
            }
        }
        if (requestParams.size() != 3) {
            throw new IllegalStateException("Invalid parameter handling in " + counterOfQueries + " query.");
        }
        return requestParams;
    }

    public static void processException(String s) {
        System.out.println(s);
        Processor.logger.writeExceptionToLogFile(s);
    }

    public static void processRequest(int counterOfQueries, Processor processor, String requestNum, String inputInfoToMatch,
                                      String outputFile) throws InputMismatchException, CustomException {
        switch (RequestName.castFromStringToEnum(requestNum)) {
            case SHORT_NAME: {
                CompanyStructure sh = processor.searchByShortName(inputInfoToMatch.toLowerCase());
                Processor.logger.writeRequestDataToLogFile(RequestName.SHORT_NAME, inputInfoToMatch, sh != null ? 1 : 0);
                outFirstToSpecifiedFile(sh, outputFile, counterOfQueries);
                break;
            }
            case TYPE_OF_WORK: {
                List<CompanyStructure> listOfSh = processor.searchByTypeOfWork(inputInfoToMatch);
                Processor.logger.writeRequestDataToLogFile(RequestName.TYPE_OF_WORK,
                        inputInfoToMatch, listOfSh.size());
                outToSpecifiedFile(listOfSh, outputFile, counterOfQueries);
                break;
            }
            case EMPLOYEES: {
                String[] emIn = inputInfoToMatch.split("[ ]+");
                try {
                    List<CompanyStructure> listOfSh = processor.searchByEmployees(
                            Integer.parseInt(emIn[0]), Integer.parseInt(emIn[1]));
                    Processor.logger.writeRequestDataToLogFile(
                            RequestName.EMPLOYEES, Arrays.toString(emIn), listOfSh.size());
                    outToSpecifiedFile(listOfSh, outputFile, counterOfQueries);
                } catch (NumberFormatException ex) {
                    throw new CustomException("Invalid bound input in " + counterOfQueries + " query.");
                }
                break;
            }
            default: {
                Processor.logger.writeRequestDataToLogFile(RequestName.DEFAULT,
                        "Invalid request type in " + counterOfQueries + " query.", 0);
                break;
            }
        }
    }

    public static void outFirstToSpecifiedFile(CompanyStructure sh, String outputFile, int counterOfQueries) {
        if (!Processor.checkOutfileForValidity(outputFile, counterOfQueries)) {
            outputFile = "request" + counterOfQueries;
        }
        try (BufferedWriter out = new BufferedWriter(new FileWriter(Processor.createFile(outputFile) + ".csv"))) {
            if (sh != null) {
                out.write(sh.toString());
            }
            out.write("\n");
        } catch (IOException | CustomException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void outToSpecifiedFile(List<CompanyStructure> listOfSh, String outputFile, int counterOfQueries) {
        if (!Processor.checkOutfileForValidity(outputFile, counterOfQueries)) {
            outputFile = "request" + counterOfQueries;
        }
        try (BufferedWriter out = new BufferedWriter(new FileWriter(Processor.createFile(outputFile) + ".csv"))) {
            for (CompanyStructure s : listOfSh) {
                out.write(s.toString());
            }
            out.write("\n");
        } catch (IOException | CustomException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
