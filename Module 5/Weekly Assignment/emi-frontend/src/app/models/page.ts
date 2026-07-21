// Mirrors Spring Data's Page<T> JSON response shape (only the fields we use).
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // current page index (0-based)
  size: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
}
