package i2.act.fuzzer;

public interface SpecificationFactory {

  public static final long DEFAULT_SEED = 1303;

  default Specification createSpecification() {
    return createSpecification(DEFAULT_SEED);
  }

  public Specification createSpecification(final long seed);

}
