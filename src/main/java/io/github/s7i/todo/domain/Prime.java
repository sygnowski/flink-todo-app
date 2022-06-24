package io.github.s7i.todo.domain;

import java.math.BigInteger;
import java.util.function.Consumer;

public class Prime {

    public static final BigInteger BIG_PRIME = new BigInteger("18446744073709551557");
    public static final long STRESS_PRIMES = 72057594037931771L;

    public static void runStressPrime(int npn) {
        genPrime(STRESS_PRIMES, npn, System.out::println);
    }

    public static void main(String[] args) {
        runStressPrime(10);
    }

    public static boolean isPrime(long n) {

        if (n <= 1 || n % 2 == 0 || n % 3 == 0) {
            return false;
        }
        if (n <= 3) {
            return true;
        }

        long i = 5L;

        for (; i * i <= n; ) {
            if (n % i == 0 || n % (i + 2) == 0) {
                return false;
            }
            i += 6L;
        }

        return true;
    }

    public static void genPrime(long startNumber, int numberOfPrimes, Consumer<Long> output) {
        long number = startNumber;
        int count = 0;
        for (; ; ) {
            if (isPrime(number)) {
                output.accept(number);
                count += 1;
            }
            number++;

            if (count >= numberOfPrimes) {
                break;
            }
        }
    }

}
