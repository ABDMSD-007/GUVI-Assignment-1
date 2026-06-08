import java.util.Scanner;

public class Main {

    public static void evenOrOdd(int n) {
        if (n % 2 == 0) {
            System.out.println("Even or Odd = Even");
        } else {
            System.out.println("Even or Odd = Odd");
        }
    }

    public static void studentGrades(int m) {
        if (m > 90) {
            System.out.println("Student Grade = A");
        } else if (m > 80) {
            System.out.println("Student Grade = B");
        } else if (m > 70) {
            System.out.println("Student Grade = C");
        } else if (m > 60) {
            System.out.println("Student Grade = D");
        } else {
            System.out.println("Student Grade = F");
        }
    }

    public static void leapYear(int y) {
        if ((y % 4 == 0 && y % 100 != 0) || (y % 400 == 0)) {
            System.out.println("Leap Year Check = Leap");
        } else {
            System.out.println("Leap Year Check = Not Leap");
        }
    }

    public static void positiveOrNegative(int n) {
        if (n > 0) {
            System.out.println("Positive/Negative Check = Positive");
        } else if (n < 0) {
            System.out.println("Positive/Negative Check = Negative");
        } else {
            System.out.println("Positive/Negative Check = Zero");
        }
    }

    public static void largestOfThree(int a, int b, int c) {
        int m = a;

        if (b > m) {
            m = b;
        }

        if (c > m) {
            m = c;
        }

        System.out.println("Greatest Number = " + m);
    }

    public static void factorsOfNumber(int n, int o) {
        int c = 0;
        int s = 0;

        switch (o) {
            case 1:
                System.out.println("Factors of " + n + " :");
                for (int i = 1; i <= n; i++) {
                    if (n % i == 0) {
                        System.out.println(i);
                    }
                }
                break;

            case 2:
                for (int i = 1; i <= n; i++) {
                    if (n % i == 0) {
                        c++;
                    }
                }
                System.out.println("Number of Factors = " + c);
                break;

            case 3:
                for (int i = 1; i <= n; i++) {
                    if (n % i == 0) {
                        s = s + i;
                    }
                }
                System.out.println("Sum of Factors = " + s);
                break;
        }
    }

    public static void primeOrNot(int n) {
        if (n <= 1) {
            System.out.println("Prime Check = Not Prime");
            return;
        }

        int c = 0;

        for (int i = 2; i < n; i++) {
            if (n % i == 0) {
                c++;
            }
        }

        if (c == 0) {
            System.out.println("Prime Check = Prime");
        } else {
            System.out.println("Prime Check = Not Prime");
        }
    }

    public static void starPatternOne() {
        System.out.println("Star Pattern 1:");

        int n = 5;
        int m = 2;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {

                int r = m - i;
                if (r < 0) {
                    r = -r;
                }

                int c = m - j;
                if (c < 0) {
                    c = -c;
                }

                if (r + c == m) {
                    System.out.print("*");
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
    }

    public static void starPatternTwo() {
        System.out.println("Star Pattern 2:");

        int n = 5;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {

                if (i == 0 || i == n - 1 || j == 0 || j == n - 1) {
                    System.out.print("*");
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
    }

    public static void fibonacciSeries(int n) {
        System.out.println("Fibonacci Series:");

        int a = 0;
        int b = 1;

        for (int i = 0; i < n; i++) {
            System.out.println(a);

            int c = a + b;
            a = b;
            b = c;
        }
    }

    public static void factorialOfNumber(int n) {
        int f = 1;

        for (int i = 1; i <= n; i++) {
            f = f * i;
        }

        System.out.println("Factorial = " + f);
    }

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        evenOrOdd(5);
        studentGrades(85);
        leapYear(2024);
        positiveOrNegative(-10);
        largestOfThree(3, 7, 5);

        factorsOfNumber(12, 1);

        primeOrNot(7);

        starPatternOne();
        starPatternTwo();

        fibonacciSeries(5);

        factorialOfNumber(5);

        sc.close();
    }
}